/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.jd.decompiler;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;
import org.sf.feeling.decompiler.JavaDecompilerPlugin;
import org.sf.feeling.decompiler.editor.IDecompiler;
import org.sf.feeling.decompiler.jd.JDCoreDecompilerPlugin;
import org.sf.feeling.decompiler.util.FileUtil;
import org.sf.feeling.decompiler.util.JarClassExtractor;
import org.sf.feeling.decompiler.util.UIUtil;

import jd.ide.eclipse.editors.JDSourceMapper;

public class JDCoreDecompiler implements IDecompiler
{

	private String source = ""; // $NON-NLS-1$ //$NON-NLS-1$
	private long time, start;
	private String log = ""; //$NON-NLS-1$

	private JDSourceMapper mapper;

	public JDCoreDecompiler( JDSourceMapper mapper )
	{
		this.mapper = mapper;
	}

	/**
	 * Performs a <code>Runtime.exec()</code> on jad executable with selected
	 * options.
	 * 
	 * @see IDecompiler#decompile(String, String, String)
	 */
	@Override
	public void decompile( String root, String classPackage, String className )
	{
		start = System.currentTimeMillis( );
		log = ""; //$NON-NLS-1$
		source = ""; //$NON-NLS-1$
		Boolean displayNumber = null;

		File workingDir = new File( root ); // $NON-NLS-1$

		File zipFile = new File( System.getProperty( "java.io.tmpdir" ), //$NON-NLS-1$
				className.replaceAll( "(?i)\\.class", System.currentTimeMillis( ) + ".jar" ) ); //$NON-NLS-1$ //$NON-NLS-2$
		String zipFileName = zipFile.getAbsolutePath( );

		try
		{
			if ( classPackage.length( ) == 0 )
			{
				DecompilerContext.initContext( new HashMap<String, Object>( ) );
				DecompilerContext.setCounterContainer( new CounterContainer( ) );
				StructClass structClass = new StructClass( FileUtil.getBytes( new File( root, className ) ),
						true,
						new LazyLoader( null ) );
				structClass.releaseResources( );
				classPackage = structClass.qualifiedName.replace( "/" //$NON-NLS-1$
						+ className.replaceAll( "(?i)\\.class", "" ), "" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			FileUtil.zipDir( workingDir, classPackage, zipFileName );

			if ( UIUtil.isDebugPerspective( ) || JavaDecompilerPlugin.getDefault( ).isDebugMode( ) )
			{
				displayNumber = JavaDecompilerPlugin.getDefault( ).isDisplayLineNumber( );
				JavaDecompilerPlugin.getDefault( ).displayLineNumber( Boolean.TRUE );
			}

			source = mapper.decompile( zipFileName,
					( classPackage.length( ) > 0 ? ( classPackage + "/" ) : "" ) //$NON-NLS-1$ //$NON-NLS-2$
							+ className );

			if ( !zipFile.delete( ) )
			{
				zipFile.deleteOnExit( );
			}
		}
		catch ( Exception e )
		{
			JavaDecompilerPlugin.logError( e, e.getMessage( ) );
		}

		if ( displayNumber != null )
		{
			JavaDecompilerPlugin.getDefault( ).displayLineNumber( displayNumber );
		}

		if ( source != null )
		{
			Pattern wp = Pattern.compile( "/\\*.+?\\*/", Pattern.DOTALL ); //$NON-NLS-1$
			Matcher m = wp.matcher( source );
			while ( m.find( ) )
			{
				if ( m.group( ).matches( "/\\*\\s+\\d*\\s+\\*/" ) ) //$NON-NLS-1$
					continue;

				String group = m.group( );
				group = group.replace( "/* ", "\t" ); //$NON-NLS-1$ //$NON-NLS-2$
				group = group.replace( " */", "" ); //$NON-NLS-1$ //$NON-NLS-2$
				group = group.replace( " * ", "\t" ); //$NON-NLS-1$ //$NON-NLS-2$

				if ( log.length( ) > 0 )
					log += "\n"; //$NON-NLS-1$
				log += group;

				source = source.replace( m.group( ), "" ); //$NON-NLS-1$

			}
		}

		time = System.currentTimeMillis( ) - start;
	}

	/**
	 * Jad doesn't support decompilation from archives. This methods extracts
	 * request class file from the specified archive into temp directory and
	 * then calls <code>decompile</code>.
	 * 
	 * @see IDecompiler#decompileFromArchive(String, String, String)
	 */
	@Override
	public void decompileFromArchive( String archivePath, String packege, String className )
	{
		start = System.currentTimeMillis( );
		File workingDir = new File(
				JavaDecompilerPlugin.getDefault( ).getPreferenceStore( ).getString( JavaDecompilerPlugin.TEMP_DIR )
						+ "/" //$NON-NLS-1$
						+ System.currentTimeMillis( ) );

		try
		{
			workingDir.mkdirs( );
			JarClassExtractor.extract( archivePath, packege, className, true, workingDir.getAbsolutePath( ) );
			decompile( workingDir.getAbsolutePath( ), packege, className ); // $NON-NLS-1$
		}
		catch ( Exception e )
		{
			JavaDecompilerPlugin.logError( e, e.getMessage( ) );
			return;
		}
		finally
		{
			FileUtil.deltree( workingDir );
		}
	}

	@Override
	public long getDecompilationTime( )
	{
		return time;
	}

	@Override
	public List getExceptions( )
	{
		return Collections.EMPTY_LIST;
	}

	/**
	 * @see IDecompiler#getLog()
	 */
	@Override
	public String getLog( )
	{
		return log;
	}

	/**
	 * @see IDecompiler#getSource()
	 */
	@Override
	public String getSource( )
	{
		return source;
	}

	@Override
	public String getDecompilerType( )
	{
		return JDCoreDecompilerPlugin.decompilerType;
	}

	@Override
	public String removeComment( String source )
	{
		return source;
	}

	@Override
	public boolean supportLevel( int level )
	{
		return level < 8;
	}

	@Override
	public boolean supportDebugLevel( int level )
	{
		return level < 8;
	}

	@Override
	public boolean supportDebug( )
	{
		return true;
	}
}