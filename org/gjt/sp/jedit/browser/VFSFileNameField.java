/*
 * VFSFileNameField.java - File name field with completion
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

//{{{ Imports
import java.util.HashSet;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.gui.HistoryTextField;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.MiscUtilities;
// funa add
import org.gjt.sp.jedit.OperatingSystem;

import org.gjt.sp.util.Log;
import org.gjt.sp.util.TaskManager;
//}}}

/**
 * @author Slava Pestov
 * @version $Id: VFSFileNameField.java 22851 2013-03-17 11:03:48Z thomasmey $
 * @since jEdit 4.2pre1 (public since 4.5pre1)
 */
public class VFSFileNameField extends HistoryTextField
{
	//{{{ VFSFileNameField constructor
	public VFSFileNameField(VFSBrowser browser, String model)
	{
		super(model);
		setEnterAddsToHistory(false);

		this.browser = browser;

		Dimension dim = getPreferredSize();
		dim.width = Integer.MAX_VALUE;
		setMaximumSize(dim);

		// Enable TAB pressed for completion instead of
		// focas traversal.
		final int FORWARD = KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;
		HashSet<AWTKeyStroke> keys = new HashSet<AWTKeyStroke>(
				getFocusTraversalKeys(FORWARD));
		keys.remove(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
		setFocusTraversalKeys(FORWARD, keys);
	} //}}}

	//{{{ processKeyEvent() method
	// Funa Edit
	public void requestFocus() {
		VFSFile[] list = browser.getBrowserView().getTable().getSelectedFiles();
		if (list.length > 0 && list[0].getType() == VFSFile.FILE) {
			setText(list[0].getPath());
			setSelectionStart(0);
			setSelectionEnd(getText().length());
		}
		browser.getBrowserView().getTable().clearSelection();
		super.requestFocus();
	}
	
	public void processKeyEvent(KeyEvent evt)
	{
		if(evt.getID() == KeyEvent.KEY_PRESSED)
		{
			//  Funa Edit
			if (ClassLoader.getSystemResource("org/gjt/sp/jedit/gui/UserKey.class") != null) {
				org.gjt.sp.jedit.gui.UserKey.consume(evt,
					org.gjt.sp.jedit.gui.UserKey.ALLOW_CTRL | org.gjt.sp.jedit.gui.UserKey.ALLOW_SHIFT,
					org.gjt.sp.jedit.gui.UserKey.ALLOW_CTRL | org.gjt.sp.jedit.gui.UserKey.ALLOW_SHIFT,
					org.gjt.sp.jedit.gui.UserKey.ALLOW_CTRL | org.gjt.sp.jedit.gui.UserKey.ALLOW_SHIFT,
					org.gjt.sp.jedit.gui.UserKey.ALLOW_CTRL | org.gjt.sp.jedit.gui.UserKey.ALLOW_SHIFT,
					true);
				if (evt.isConsumed()) {
					return;
				}
			}
			String path = getText();

			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_TAB:
				doComplete(path);
				break;
			case KeyEvent.VK_LEFT:
				if ((evt.getModifiers() & KeyEvent.ALT_MASK) > 0)
				{
					browser.previousDirectory();
					evt.consume();
					// Funa edit
				} else if (getCaretPosition() == 0) {
					browser.getBrowserView().getTable().processKeyEvent(evt);
				}
				else
				{
					// 				browser.getBrowserView().getTable().processKeyEvent(evt);
					super.processKeyEvent(evt);
				}
				break;
			case KeyEvent.VK_UP:
				if ((evt.getModifiers() & KeyEvent.ALT_MASK)>0)
				{
					String p = browser.getDirectory();
					browser.setDirectory(MiscUtilities.getParentOfPath(p));
					evt.consume();
				}
				else
				{
					browser.getBrowserView().getTable()
					.processKeyEvent(evt);
				}
				break;
			case KeyEvent.VK_RIGHT:
				if ((evt.getModifiers() & KeyEvent.ALT_MASK)>0)
				{
					evt.consume();
					browser.nextDirectory();
					// Funa edit
				} else if (getCaretPosition() == getDocument().getLength()) {
					// System.out.println("test");
					browser.getBrowserView().getTable().processKeyEvent(evt);
				}
				else
					super.processKeyEvent(evt);
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_PAGE_DOWN:
				browser.getBrowserView().getTable()
					.processKeyEvent(evt);
				break;
			case KeyEvent.VK_ENTER:
				// Funa Edit
				if (evt.isAltDown()){
					if (!OperatingSystem.isMacOS() && (OperatingSystem.isWindows() || OperatingSystem.isUnix())){
						org.gjt.sp.jedit.GUIUtilities.getRobot().keyPress(KeyEvent.VK_ALT);
					}
				}
				browser.filesActivated(
					(evt.isShiftDown()
					? VFSBrowser.M_OPEN_NEW_VIEW
					: VFSBrowser.M_OPEN),false);
				setText(null);
				evt.consume();
				break;
			default:
				super.processKeyEvent(evt);
				break;
			}
		}
		else if(evt.getID() == KeyEvent.KEY_TYPED)
		{
			char ch = evt.getKeyChar();
			// Funa Edit
			// if(ch > 0x20 && ch != 0x7f && ch != 0xff)
			if(ch > 0x20 && ch != 0x7f && ch != 0xff && !evt.isAltDown())
			{
				super.processKeyEvent(evt);
				String path = getText();
				BrowserView view = browser.getBrowserView();
				view.selectNone();

				if(MiscUtilities.getLastSeparatorIndex(path) == -1)
				{
					int mode = browser.getMode();
					// fix for bug #765507
					// we don't type complete in save dialog
					// boxes. Press TAB to do an explicit
					// complete
					view.getTable().doTypeSelect(path,
						mode == VFSBrowser
						.CHOOSE_DIRECTORY_DIALOG
						||
						mode == VFSBrowser
						.SAVE_DIALOG);
				}
			}
			else
				super.processKeyEvent(evt);
		}
	} //}}}

	//{{{ Private members
	private VFSBrowser browser;

	//{{{ doComplete() method
	public String doComplete(String path, String complete, boolean dirsOnly)
	{
		Log.log(Log.DEBUG,VFSFileNameField.class,
			"doComplete(" + path + "," + complete
			+ "," + dirsOnly);

		for(;;)
		{
			if(complete.length() == 0)
				return path;
			int index = MiscUtilities.getFirstSeparatorIndex(complete);
			if(index == -1)
				return path;

			/* Until the very last path component, we only complete on
			directories */
			String newPath = VFSFile.findCompletion(path,
				complete.substring(0,index),browser,true);
			if(newPath == null)
				return null;
			path = newPath;
			complete = complete.substring(index + 1);
		}
	} //}}}

	//{{{ doComplete() method
	private void doComplete(String currentText)
	{
		int index = MiscUtilities.getLastSeparatorIndex(currentText);
		String dir;
		if(index != -1)
			dir = currentText.substring(0,index + 1);
		else
			dir = "";

		if(MiscUtilities.isAbsolutePath(currentText))
		{
			if(dir.startsWith("/"))
				dir = dir.substring(1);
			dir = doComplete(VFSBrowser.getRootDirectory(),dir,false);
			if(dir == null)
				return;

			browser.setDirectory(dir);
			TaskManager.instance.waitForIoTasks();

			if(index == -1)
			{
				if(currentText.startsWith("/"))
					currentText = currentText.substring(1);
			}
			else
				currentText = currentText.substring(index + 1);
		}
		else
		{
			if(dir.length() != 0)
			{
				dir = doComplete(browser.getDirectory(),dir,false);
				if(dir == null)
					return;

				browser.setDirectory(dir);
				TaskManager.instance.waitForIoTasks();

				currentText = currentText.substring(index + 1);
			}
		}

		BrowserView view = browser.getBrowserView();
		view.selectNone();
		view.getTable().doTypeSelect(currentText,
			browser.getMode() == VFSBrowser
			.CHOOSE_DIRECTORY_DIALOG);

		String newText;

		VFSFile[] files = view.getSelectedFiles();
		if(files.length == 0)
			newText = currentText;
		else
		{
			String path = files[0].getPath();
			String name = files[0].getName();
			String parent = MiscUtilities.getParentOfPath(path);

			if(MiscUtilities.isAbsolutePath(currentText)
				&& !currentText.startsWith(browser.getDirectory()))
			{
				newText = path;
			}
			else
			{
				if(MiscUtilities.pathsEqual(parent,browser.getDirectory()))
					newText = name;
				else
					newText = path;
			}
		}

		setText(newText);
	} //}}}

	//{{{ goToParent() method
	private void goToParent()
	{
		String name = MiscUtilities.getFileName(browser.getDirectory());
		String parent = MiscUtilities.getParentOfPath(
			browser.getDirectory());
		browser.setDirectory(parent);

		VFS vfs = VFSManager.getVFSForPath(parent);
		if((vfs.getCapabilities() & VFS.LOW_LATENCY_CAP) != 0)
		{
			TaskManager.instance.waitForIoTasks();
			setText(name);
			browser.getBrowserView().getTable().doTypeSelect(
				name,browser.getMode() == VFSBrowser
				.CHOOSE_DIRECTORY_DIALOG);
		}
	} //}}}

	//}}}
}
