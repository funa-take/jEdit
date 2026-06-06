package org.gjt.sp.jedit.gui;

import org.gjt.sp.jedit.browser.VFSDirectoryEntryTable;
import org.gjt.sp.jedit.browser.VFSFileNameField;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Alt + i/j/k/l などのユーザー定義キーを、カーソル移動・編集・ESC などの
 * 標準キーへ変換する処理を、アプリ全体で一元的に行う KeyEventDispatcher。
 *
 * 変換対象のコンポーネントは次の方法で識別する。
 * <ul>
 *   <li>テキスト入力欄・テーブルは型（instanceof）で判定</li>
 *   <li>リスト・ツリーはクライアントプロパティ {@link #MODE_PROPERTY} のタグで判定</li>
 *   <li>補完ポップアップはウィンドウ先祖が {@link CompletionPopup} かで判定</li>
 * </ul>
 * 対象外のコンポーネント（メインのテキストエリア等）には一切作用しない。
 */
public class UserKeyDispatcher implements KeyEventDispatcher
{
	/** 変換モードを指定するクライアントプロパティ名。値は {@link #MODE_NAV} / {@link #MODE_PLAIN}。 */
	public static final String MODE_PROPERTY = "userKey.mode";

	/** テーブル/ツリー向け。矢印は Ctrl/Shift 併用可、履歴(Alt+,)なし。 */
	public static final String MODE_NAV = "nav";

	/** リスト/ポップアップ向け。矢印は Alt 単独のみ、履歴(Alt+,)なし。 */
	public static final String MODE_PLAIN = "plain";

	/** 変換の起点となる修飾キー（Alt）。 */
	private static final int TRIGGER_MASK = InputEvent.ALT_DOWN_MASK;

	/** Ctrl 併用許可ビット（旧 UserKey.ALLOW_CTRL 相当）。 */
	private static final int ALLOW_CTRL = 1;

	/** Shift 併用許可ビット（旧 UserKey.ALLOW_SHIFT 相当）。 */
	private static final int ALLOW_SHIFT = 2;

	/** ルール: テキスト入力欄（Ctrl/Shift 併用可・履歴あり）。 */
	private static final int RULE_TEXT_HISTORY = 0;
	/** ルール: テーブル/ツリー（Ctrl/Shift 併用可・履歴なし）。 */
	private static final int RULE_NAV = 1;
	/** ルール: リスト/ポップアップ（Alt 単独のみ・履歴なし）。 */
	private static final int RULE_PLAIN = 2;
	/** ルール: 対象外。 */
	private static final int RULE_NONE = -1;

	private static boolean installed;

	//{{{ install() method
	/**
	 * KeyEventDispatcher を登録する。GUI 初期化時に一度だけ呼ぶ。
	 */
	public static synchronized void install()
	{
		if (installed)
			return;
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addKeyEventDispatcher(new UserKeyDispatcher());
		installed = true;
	} //}}}

	//{{{ isInstalled() method
	/**
	 * @return Dispatcher が登録済みかどうか。
	 */
	public static boolean isInstalled()
	{
		return installed;
	} //}}}

	//{{{ dispatchKeyEvent() method
	@Override
	public boolean dispatchKeyEvent(KeyEvent e)
	{
		if (e.getID() != KeyEvent.KEY_PRESSED || e.isConsumed())
			return false;
		int rule = resolveRule(e.getComponent());
		if (rule == RULE_NONE)
			return false;
		return handle(e, rule == RULE_TEXT_HISTORY || rule == RULE_NAV,
			rule == RULE_TEXT_HISTORY);
	} //}}}

	//{{{ handleMenuKey() method
	/**
	 * メニュー(JPopupMenu)が開いている間のキー変換に使う。
	 * 履歴ドロップダウンの MenuKeyListener から呼ぶ。
	 *
	 * @param e メニューのキーイベント
	 * @return 変換して消費した場合は true
	 */
	public static boolean handleMenuKey(KeyEvent e)
	{
		if (e.isConsumed())
			return false;
		return handle(e, false, false);
	} //}}}

	//{{{ resolveRule() method
	private static int resolveRule(Component c)
	{
		if (c == null)
			return RULE_NONE;

		Window w = SwingUtilities.getWindowAncestor(c);
		if (w instanceof CompletionPopup)
			return RULE_PLAIN;

		if (c instanceof JComponent)
		{
			Object mode = ((JComponent)c).getClientProperty(MODE_PROPERTY);
			if (MODE_NAV.equals(mode))
				return RULE_NAV;
			if (MODE_PLAIN.equals(mode))
				return RULE_PLAIN;
		}

		// VFSFileNameField は HistoryTextField のサブクラスなので先に判定する
		if (c instanceof VFSFileNameField)
			return RULE_NAV;
		if (c instanceof HistoryTextField || c instanceof HistoryTextArea)
			return RULE_TEXT_HISTORY;
		if (c instanceof VFSDirectoryEntryTable)
			return RULE_NAV;

		return RULE_NONE;
	} //}}}

	//{{{ handle() method
	private static boolean handle(KeyEvent e, boolean allowCtrlShift, boolean history)
	{
		int mods = KeyEventTranslator.translateModifiersEx(e.getModifiersEx());
		int arrowMod = allowCtrlShift ? (ALLOW_CTRL | ALLOW_SHIFT) : 0;
		int target = -1;
		int newMods = mods & ~TRIGGER_MASK;

		switch (e.getKeyCode())
		{
		case KeyEvent.VK_I:
			if (isConsume(mods, arrowMod)) target = KeyEvent.VK_UP;
			break;
		case KeyEvent.VK_K:
			if (isConsume(mods, arrowMod)) target = KeyEvent.VK_DOWN;
			break;
		case KeyEvent.VK_J:
			if (isConsume(mods, arrowMod)) target = KeyEvent.VK_LEFT;
			break;
		case KeyEvent.VK_L:
			if (isConsume(mods, arrowMod)) target = KeyEvent.VK_RIGHT;
			break;
		case KeyEvent.VK_PERIOD:
			if (isConsume(mods, arrowMod)) target = KeyEvent.VK_PAGE_DOWN;
			break;
		case KeyEvent.VK_O:
			if (isConsume(mods, arrowMod)) target = KeyEvent.VK_PAGE_UP;
			break;
		case KeyEvent.VK_COLON:
			if (isConsume(mods, arrowMod)) target = KeyEvent.VK_END;
			break;
		case KeyEvent.VK_SEMICOLON:
			if (isConsume(mods, arrowMod)) target = KeyEvent.VK_HOME;
			break;
		case KeyEvent.VK_H:
			if (isConsume(mods, 0)) target = KeyEvent.VK_ESCAPE;
			break;
		case KeyEvent.VK_0:
			if (isConsume(mods, ALLOW_CTRL)) target = KeyEvent.VK_DELETE;
			break;
		case KeyEvent.VK_9:
			if (isConsume(mods, ALLOW_CTRL)) target = KeyEvent.VK_BACK_SPACE;
			break;
		case KeyEvent.VK_COMMA:
			if (history && isConsume(mods, ALLOW_CTRL | ALLOW_SHIFT))
			{
				target = KeyEvent.VK_DOWN;
				newMods = (mods & ~TRIGGER_MASK)
					| KeyEventTranslator.getModifierBeforeTranslateEx(KeyEvent.ALT_DOWN_MASK);
			}
			break;
		}

		if (target < 0)
			return false;

		Component src = e.getComponent();
		KeyEvent ne = new KeyEvent(src, e.getID(), e.getWhen(), newMods,
			target, KeyEvent.CHAR_UNDEFINED);
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(ne);
		e.consume();
		return true;
	} //}}}

	//{{{ isConsume() method
	private static boolean isConsume(int mods, int mod)
	{
		if ((mods & TRIGGER_MASK) == 0)
			return false;
		if ((mods & InputEvent.CTRL_DOWN_MASK) != 0 && (mod & ALLOW_CTRL) == 0)
			return false;
		if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0 && (mod & ALLOW_SHIFT) == 0)
			return false;
		return true;
	} //}}}
}
