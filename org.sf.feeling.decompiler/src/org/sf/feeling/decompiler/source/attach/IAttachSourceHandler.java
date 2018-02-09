/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.source.attach;

import org.eclipse.jdt.core.IPackageFragmentRoot;

public interface IAttachSourceHandler
{

	public void execute( IPackageFragmentRoot library, boolean showUI );

	public boolean syncAttachSource( IPackageFragmentRoot root );

}
