package org.gjt.sp.jedit.gui;

import org.gjt.sp.jedit.OperatingSystem;
import java.awt.event.*;
import java.awt.*;

/**
 *  Description of the Class
 *
 *@author    Takeshi Funayama
 */
public class UserKey {
  /**  コントロールキー判定用定数 KeyEvent.CTRL_MASKに変更予定？ */
  public final static int ALLOW_CTRL = 1;
  /**  シフトキー判定用定数 KeyEvent.CTRL_SHIFT？ */
  public final static int ALLOW_SHIFT = 2;
  public final static int MyMASK = InputEvent.ALT_DOWN_MASK;
  // public static final int ALT_NO_CENCEL = 1;
  // private static final int MyMASK = (OperatingSystem.isMacOS() ? KeyEvent.CTRL_MASK : KeyEvent.ALT_MASK);
  
  /**
   *  Gets the consume attribute of the UserKey class
   *
   *@param  evt  Description of the Parameter
   *@param  mod  Description of the Parameter
   *@return      The consume value
   */
  private static boolean isConsume(int modifiers, int mod) {
    if ((modifiers & MyMASK) == 0) {
      return false;
    }
    if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0 && (mod & ALLOW_CTRL) == 0) {
      return false;
    }
    
    if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0 && (mod & ALLOW_SHIFT) == 0) {
      return false;
    }
    
    return true;
  }
  
  
  /**
   *  ユーザー用のキー入力変換の変換を行う。
   *  例：Alt + j -> 「←キー」
   *
   *@param  evt             キーイベント
   *@param  mod_up          SHIFTまたは、CTRLが押されていたときに、「↑キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  mod_right       SHIFTまたは、CTRLが押されていたときに、「→キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  mod_down        SHIFTまたは、CTRLが押されていたときに、「↓キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  mod_left        SHIFTまたは、CTRLが押されていたときに、「←キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  blnUP_DOWN      「↑キー」、「下キー」、「Page Up」、「Page Down」に変換をするかどうか。（カーソルを左右にだけ動かしたいときに）
   *@param  blnLEFT_REIGHT  「←キー」、「→キー」、「Home」、「End」に変換をするかどうか。（カーソルを上下にだけ動かしたいときに）
   *@param  blnEdit         「delキー」、「BSキー」に変換するかどうか（編集を行うかどうか）
   *@param  blnESC          「ESCキー」に変換するかどうか
   */
  public static void consume(KeyEvent evt, int mod_up, int mod_right, int mod_down, int mod_left,
    boolean blnUP_DOWN, boolean blnLEFT_REIGHT, boolean blnEdit, boolean blnESC, boolean blnHistory) 
  {
    int translateModifiers = KeyEventTranslator.translateModifiersEx(evt.getModifiersEx());
    switch (evt.getKeyCode()) {
    case KeyEvent.VK_I:
      if (blnUP_DOWN && isConsume(translateModifiers, mod_up)) {
        // if (evt.isAltDown() && !evt.isAltGraphDown()
        // && !evt.isMetaDown() && !evt.isShiftDown()) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    case KeyEvent.VK_K:
      if (blnUP_DOWN && isConsume(translateModifiers, mod_down)) {
        // if (evt.isAltDown() && !evt.isAltGraphDown()
        // && !evt.isMetaDown() && !evt.isShiftDown()) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    case KeyEvent.VK_J:
      if (blnLEFT_REIGHT && isConsume(translateModifiers, mod_left)) {
        // if (evt.isAltDown() && !evt.isAltGraphDown()
        // && !evt.isControlDown() && !evt.isMetaDown() ) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    case KeyEvent.VK_L:
      if (blnLEFT_REIGHT && isConsume(translateModifiers, mod_right)) {
        // if (evt.isAltDown() && !evt.isAltGraphDown()
        // && !evt.isControlDown() && !evt.isMetaDown()
        // && !evt.isShiftDown()) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
      // case KeyEvent.VK_N:
    case KeyEvent.VK_PERIOD:
      if (blnUP_DOWN && isConsume(translateModifiers, mod_down)) {
        // if (evt.isAltDown() && !evt.isAltGraphDown()
        // && !evt.isControlDown() && !evt.isMetaDown()
        // && !evt.isShiftDown()) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_PAGE_DOWN, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
      // case KeyEvent.VK_H:
    case KeyEvent.VK_O:
      if (blnUP_DOWN && isConsume(translateModifiers, mod_up)) {
        // if (evt.isAltDown() && !evt.isAltGraphDown()
        // && !evt.isControlDown() && !evt.isMetaDown()
        // && !evt.isShiftDown()) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_PAGE_UP, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    case KeyEvent.VK_H:
      if (blnESC && isConsume(translateModifiers, 0)) {
        Toolkit.getDefaultToolkit()
        .getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    case KeyEvent.VK_COLON:
      if (blnLEFT_REIGHT && isConsume(translateModifiers, mod_right)) {
        // if (evt.isAltDown() && !evt.isAltGraphDown()
        // && !evt.isControlDown() && !evt.isMetaDown()
        // && !evt.isShiftDown()) {
        Toolkit.getDefaultToolkit()
        .getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_END, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    case KeyEvent.VK_SEMICOLON:
      if (blnLEFT_REIGHT && isConsume(translateModifiers, mod_left)) {
        // if (evt.isAltDown() && !evt.isAltGraphDown()
        // && !evt.isControlDown() && !evt.isMetaDown()
        // && !evt.isShiftDown()) {
        Toolkit.getDefaultToolkit()
        .getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_HOME, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    case KeyEvent.VK_0:
      if (blnEdit && isConsume(translateModifiers, ALLOW_CTRL)) {
        Toolkit.getDefaultToolkit()
        .getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    case KeyEvent.VK_9:
      if (blnEdit && isConsume(translateModifiers, ALLOW_CTRL)) {
        Toolkit.getDefaultToolkit()
        .getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK,
            KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    case KeyEvent.VK_COMMA:
      if (blnHistory && isConsume(translateModifiers, ALLOW_CTRL | ALLOW_SHIFT)) {
        Toolkit.getDefaultToolkit()
        .getSystemEventQueue().postEvent(
          new KeyEvent((Component)evt.getSource(), evt.getID(),
            evt.getWhen(), translateModifiers & ~MyMASK | KeyEvent.ALT_MASK,
            KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED)
          );
        evt.consume();
      }
      break;
    }
  }
  
  
  /**
   *  ユーザー用のキー入力変換の変換を行う。
   *  例：Alt + j -> 「←キー」
   *
   *@param  evt        キーイベント
   *@param  mod_up          SHIFTまたは、CTRLが押されていたときに、「↑キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  mod_right       SHIFTまたは、CTRLが押されていたときに、「→キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  mod_down        SHIFTまたは、CTRLが押されていたときに、「↓キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  mod_left        SHIFTまたは、CTRLが押されていたときに、「←キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  blnESC          「ESCキー」に変換するかどうか
   */
  public static void consume(KeyEvent evt, int mod_up, int mod_right, int mod_down, int mod_left, boolean blnESC) {
    UserKey.consume(evt, mod_up, mod_right, mod_down, mod_left, true, true, true, blnESC,false);
  }
  
  
  /**
   *  ユーザー用のキー入力変換の変換を行う。
   *  例：Alt + j -> 「←キー」
   *
   *@param  evt        キーイベント
   *@param  mod_up          SHIFTまたは、CTRLが押されていたときに、「↑キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  mod_right       SHIFTまたは、CTRLが押されていたときに、「→キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  mod_down        SHIFTまたは、CTRLが押されていたときに、「↓キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  mod_left        SHIFTまたは、CTRLが押されていたときに、「←キー」に変換するかどうか。ALLOW_CTRL、ALLOW_SHIFTで指定。
   *@param  blnEdit         「delキー」、「BSキー」に変換するかどうか（編集を行うかどうか）
   *@param  blnESC          「ESCキー」に変換するかどうか
   */
  public static void consume(KeyEvent evt, int mod_up, int mod_right, int mod_down, int mod_left, boolean blnEdit, boolean blnESC) {
    UserKey.consume(evt, mod_up, mod_right, mod_down, mod_left, true, true, blnEdit, blnESC,false);
  }
}

