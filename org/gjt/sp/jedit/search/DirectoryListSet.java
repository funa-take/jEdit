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
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.StandardUtilities;
import java.util.List;
import java.util.ArrayList;
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
			return _getFilesUseGrep(comp, skipBinary, skipHidden, useGrep);
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
	
	protected String[] _getFilesUseGrep(final Component comp, boolean skipBinary, boolean skipHidden, boolean useGrep) {
		// macで regextype オプションが使えないため、grepでファイルを絞り込み
		// find "./te st" -type f -regextype posix-egrep  -iregex ".?(/[^.][^/] *)*(javA|text)" -print0 | xargs -0 grep -l  -i -E $'TEST'
		// 以下のコマンドを実行する
		// find "." -type f | grep -i -E "^.?(/[^.][^/]*)*$" | grep -i -E ".*(java|text)" | sed -e 's/ /\\ /g' | xargs grep -l  -i -E $'hoge4'
		StringBuilder sb = new StringBuilder();
		sb.append("find ");
		sb.append("\"").append(directory).append("\" ");
		sb.append("-type f ");
		if (!recurse) {
			sb.append("-maxdepth 1 ");
		} 
		
		if (skipHidden) {
			sb.append(" | grep -E \"^(/[^.][^/]*)*$\"");
		}
		
		sb.append(" | grep -i -E \"").append(StandardUtilities.globToRE(glob)).append("$\" ");
		sb.append(" | sed -e 's/ /\\\\ /g' ");
		
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
			sb.append("-P $'").append(searchString).append("' ");
		} else {
			searchString = searchString.replaceAll("\"", "\\\\\"");
			sb.append("-F \"").append(searchString).append("\" ");
		}
		
		sb.append("| sort ");
		System.out.println(sb.toString());
		
		ArrayList<String> commands = new ArrayList();
		commands.add("/bin/sh");
		commands.add("-c");
		commands.add(sb.toString());
		
		try {
			String result = exec(commands, "UTF-8", "UTF-8");
			if ("".equals(result)) {
				return null;
			} 
			return result.split("\n");
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
		return null;
	}
	
	public static String exec(List<String> command, String outEncoding, String inEncoding) throws IOException {
		String lineSep = "\n";
		BufferedReader pbr = null;
		BufferedReader pbe = null;
		StringBuffer result = new StringBuffer();
		
		try {
			String[] commandArray = new String[command.size()];
			command.toArray(commandArray);
			
			Runtime runtime = Runtime.getRuntime();
			Process p = runtime.exec(commandArray);
			
			pbr = new BufferedReader(new InputStreamReader(p.getInputStream(), inEncoding));
			pbe = new BufferedReader(new InputStreamReader(p.getErrorStream(), inEncoding));
			
			String line = null;
			while ( (line = pbr.readLine()) != null){
				result.append(line);
				result.append(lineSep);
			}
			pbr.close();
			
			while ( (line = pbe.readLine()) != null){
				System.err.println(line);
			}
			pbe.close();
			
		} finally {
			close(pbr);
			close(pbe);
		}
		
		return result.toString();
	}
  
  public static void close(Closeable closeable) {
  	  try {
  	  	  if (closeable != null) {
  	  	  	  closeable.close();
  	  	  }
  	  } catch (Exception e) {
  	  }
  }
	
	//}}}

	//{{{ Private members
	private String directory;
	private String glob;
	private boolean recurse;
	//}}}
}
