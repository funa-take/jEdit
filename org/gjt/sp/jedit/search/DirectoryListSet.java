/*
* DirectoryListSet.java - Directory list matcher
* :tabSize=4:indentSize=4:noTabs=false:
* :folding=explicit:collapseFolds=1:
*
* Copyright (C) 1999, 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.search;

//{{{ Imports
import java.awt.Component;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
//}}}

/**
 * Recursive directory search.
 * @author Slava Pestov
 * @version $Id: DirectoryListSet.java 22454 2012-11-10 11:15:08Z thomasmey $
 */
public class DirectoryListSet extends BufferListSet
{
	//{{{ DirectoryListSet constructor
	public DirectoryListSet(String directory, String glob, boolean recurse)
	{
		this.directory = directory;
		this.glob = glob;
		this.recurse = recurse;
	} //}}}
	
	
	
	//{{{ getDirectory() method
	public String getDirectory()
	{
		return directory;
	} //}}}
	
	//{{{ setDirectory() method
	/**
	 * @since jEdit 4.2pre1
	 */
	public void setDirectory(String directory)
	{
		this.directory = directory;
		invalidateCachedList();
	} //}}}
	
	//{{{ getFileFilter() method
	public String getFileFilter()
	{
		return glob;
	} //}}}
	
	//{{{ setFileFilter() method
	/**
	 * @since jEdit 4.2pre1
	 */
	public void setFileFilter(String glob)
	{
		this.glob = glob;
		invalidateCachedList();
	} //}}}
	
	//{{{ isRecursive() method
	public boolean isRecursive()
	{
		return recurse;
	} //}}}
	
	//{{{ setRecursive() method
	/**
	 * @since jEdit 4.2pre1
	 */
	public void setRecursive(boolean recurse)
	{
		this.recurse = recurse;
		invalidateCachedList();
	} //}}}
	
	//{{{ getCode() method
	@Override
	public String getCode()
	{
		return "new DirectoryListSet(\"" + StandardUtilities.charsToEscapes(directory)
		+ "\",\"" + StandardUtilities.charsToEscapes(glob) + "\","
		+ recurse + ')';
	} //}}}
	
	//{{{ _getFiles() method
	@Override
	protected String[] _getFiles(final Component comp)
	{
		boolean skipBinary, skipHidden, useGrep;
		skipBinary = jEdit.getBooleanProperty("search.skipBinary.toggle");
		skipHidden = jEdit.getBooleanProperty("search.skipHidden.toggle");
		useGrep = jEdit.getBooleanProperty("search.useGrep.toggle");
		
		if (useGrep 
			&& (!MiscUtilities.isURL(directory) 
				|| "sftp".equals(MiscUtilities.getProtocolOfURL(directory))
				)
			)
		{
			return _getFilesUseGrep(comp, skipBinary, skipHidden);
		} else {
			return _getFilesUseVFS(comp, skipBinary, skipHidden);
		}
		
	}
	
	protected String[] _getFilesUseVFS(final Component comp, boolean skipBinary, boolean skipHidden) {
		final VFS vfs = VFSManager.getVFSForPath(directory);
		Object session;
		session = vfs.createVFSSessionSafe(directory, comp);
		
		try
		{
			try
			{
				return vfs._listDirectory(session,directory,glob,recurse,comp, skipBinary, skipHidden);
			}
			finally
			{
				vfs._endVFSSession(session, comp);
			}
		}
		catch(IOException io)
		{
			VFSManager.error(comp,directory,"ioerror",new String[]
				{ io.toString() });
			return null;
		}
	}
	
	protected String[] _getFilesUseGrep(final Component comp, boolean skipBinary, boolean skipHidden) {
		try {
			if (MiscUtilities.isURL(directory) && "sftp".equals(MiscUtilities.getProtocolOfURL(directory))) {
				return grepForRemote(skipBinary, skipHidden);
			} else {
				return grepForLocal(skipBinary, skipHidden);
			}
		} catch (Exception e){
			// e.printStackTrace();
			Log.log(Log.ERROR,DirectoryListSet.class,e);
		}	
		return null;
	}
	
	private String[] grepForLocal(boolean skipBinary, boolean skipHidden) throws Exception {
		ClassLoader cl = jEdit.getPlugin("funa.util.FunaUtilPlugin").getPluginJAR().getClassLoader();
		Class miscutil = Class.forName("funa.util.MiscUtil", true, cl);
		Class execResult = Class.forName("funa.util.ExecResult", true, cl);
		Method method = miscutil.getDeclaredMethod("exec", List.class, String.class, String.class);
		
		ArrayList<String> commands = new ArrayList();
		boolean forMsys2 = OperatingSystem.isWindows();
		
		if (forMsys2) {
			commands.add("cmd");
			commands.add("/c");
		} else {
			commands.add("/bin/sh");
			commands.add("-c");
		}
		String command = createGrepCommand(directory, skipBinary, skipHidden, forMsys2);
		Log.log(Log.MESSAGE,DirectoryListSet.class,command);
		commands.add(command);
		Object result = method.invoke(null, commands, "", "UTF-8");
		
		String stdOut = (String)(execResult.getMethod("getStdOut")).invoke(result);
		String stdErr = (String)(execResult.getMethod("getStdErr")).invoke(result);
		
		if (!"".equals(stdErr)) {
			Log.log(Log.ERROR,DirectoryListSet.class, stdErr);
		}
		
		if ("".equals(stdOut)) {
			return null;
		} 
		String[] paths = stdOut.split("\n");
		if (skipHidden) {
			paths = skipBackup(paths);
		}
		
		return paths;
	}
	
	private String[] grepForRemote(boolean skipBinary, boolean skipHidden) throws Exception{
		ClassLoader cl = jEdit.getPlugin("funa.util.FunaUtilPlugin").getPluginJAR().getClassLoader();
		Class miscutil = Class.forName("funa.util.MiscUtilForSsh", true, cl);
		Class execResult = Class.forName("funa.util.ExecResult", true, cl);
		Method method = miscutil.getDeclaredMethod("exec", String.class, List.class, String.class, String.class);
		
		int startDir = directory.indexOf("/", "sftp://".length());
		String protocolAndHostInfo = directory.substring(0, startDir);
		String hostInfo = protocolAndHostInfo.substring("sftp://".length(), startDir);
		String remoteDirectory = directory.substring(startDir);
		
		ArrayList<String> commands = new ArrayList();
		String command = createGrepCommand(remoteDirectory, skipBinary, skipHidden);
		Log.log(Log.MESSAGE,DirectoryListSet.class,command);
		commands.add(command);
		Object result = method.invoke(null, hostInfo, commands, "", "UTF-8");
		
		String stdOut = (String)(execResult.getMethod("getStdOut")).invoke(result);
		String stdErr = (String)(execResult.getMethod("getStdErr")).invoke(result);
		
		if (!"".equals(stdErr)) {
			Log.log(Log.ERROR,DirectoryListSet.class, stdErr);
		}
		
		if ("".equals(stdOut)) {
			return null;
		}
		
		String[] paths = stdOut.split("\n");
		
		String prefix = directory.substring(0, startDir);
		for(int i = 0; i < paths.length; i++) {
			paths[i] = protocolAndHostInfo + paths[i];
		}
		
		if (skipHidden) {
			paths = skipBackup(paths);
		}
		
		return paths;
	}
	
	private String[] skipBackup(String[] paths) {
		ArrayList<String> result = new ArrayList<String>();
		for(String path: paths) {
			if (!MiscUtilities.isBackup(path)) {
				result.add(path);
			}
		}
		return result.toArray(new String[]{});
	}
	
	private String createGrepCommand(String searchDirectory, boolean skipBinary, boolean skipHidden) {
		return createGrepCommand(searchDirectory, skipBinary, skipHidden, false);
	}
	private String createGrepCommand(String searchDirectory, boolean skipBinary, boolean skipHidden, boolean forMsys2) {
		// macで regextype オプションが使えないため、grepでファイルを絞り込み
		// find "./te st" -type f -regextype posix-egrep  -iregex ".?(/[^.][^/] *)*(javA|text)" -print0 | xargs -0 grep -l  -i -E $'TEST'
		// 以下のコマンドを実行する
		// find "." -type f | grep -i -E "^.?(/[^.][^/]*)*$" | grep -i -E ".*(java|text)" | sed -e 's/ /\\ /g' | xargs grep -l  -i -E $'hoge4'
		
		// Windowsでmsys2を使う場合
		// find "C:\data\temp\temp" -type f | sed -e 's/\\\\/\//g' | grep -E "^(.:)?(/[^.][^/]*)*$" | grep -i -E ".*$"  | sed -e 's/.*/"\\0"/g' | xargs grep -l -i -I -E 'hoge'
		
		StringBuilder sb = new StringBuilder();
		sb.append("find ");
		sb.append("\"").append(searchDirectory).append("\" ");
		sb.append("-type f ");
		if (!recurse) {
			sb.append("-maxdepth 1 ");
		}
		
		if (forMsys2) {
			sb.append(" | sed -e 's/\\\\\\\\/\\//g'");
		}
		
		if (skipHidden) {
			if (forMsys2) {
				sb.append(" | grep -E \"^(.:)?(/[^.][^/]*)*$\"");
			} else {
				sb.append(" | grep -E \"^(/[^.][^/]*)*$\"");
			}
		}
		
		sb.append(" | grep -i -E \"").append(StandardUtilities.globToRE(glob)).append("$\" ");
		
		if (forMsys2) {
			sb.append(" | sed -e 's/.*/\"\\\\0\"/g' ");
		} else {
			sb.append(" | sed -e 's/ /\\\\ /g' ");
		}
		
		sb.append(" | xargs grep -l ");
		if (SearchAndReplace.getIgnoreCase()) {
			sb.append("-i ");
		}
		if (SearchAndReplace.getWholeWord()) {
			sb.append("-w ");
		}
		if (skipBinary) {
			sb.append("-I ");
		}
		
		String searchString = SearchAndReplace.getSearchString();
		if (SearchAndReplace.getRegexp()) {
			// searchString = searchString.replaceAll("\n", "\\\\n");
			// searchString = searchString.replaceAll("\\\\\\\\", "\\\\\\\\\\\\\\\\");
			// searchString = searchString.replaceAll("'", "\\\\'");
			// sb.append("-E $'").append(searchString).append("' ");
			
			searchString = searchString.replaceAll("\\\\\\\\", "\\\\\\\\\\\\\\\\");
			searchString = searchString.replaceAll("'", "\\\\'");
			if (forMsys2) {
				sb.append("-E '");
			} else {
				sb.append("-E $'");
			}
			sb.append(searchString).append("' ");
			// sb.append("-P $'").append(searchString).append("' ");
		} else {
			searchString = searchString.replaceAll("\"", "\\\\\"");
			sb.append("-F \"").append(searchString).append("\" ");
		}
		
		sb.append("| sort -f ");
		
		return sb.toString();
	}
	
	//}}}
	
	//{{{ Private members
	private String directory;
	private String glob;
	private boolean recurse;
	//}}}
}
