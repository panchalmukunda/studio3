/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.common.outline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import com.aptana.core.util.ArrayUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.resolver.IPathResolver;
import com.aptana.parsing.IParseState;
import com.aptana.parsing.ast.IParseNode;

public class CommonOutlineContentProvider implements ITreeContentProvider
{

	protected static final Object[] EMPTY = ArrayUtil.NO_OBJECTS;
	private IParseListener fListener;
	protected IPathResolver resolver;

	private boolean autoExpanded;

	public CommonOutlineItem getOutlineItem(IParseNode node)
	{
		if (node == null)
		{
			return null;
		}
		return new CommonOutlineItem(node.getNameNode().getNameRange(), node);
	}

	public Object[] getChildren(Object parentElement)
	{
		if (parentElement instanceof AbstractThemeableEditor)
		{
			IParseNode rootNode = ((AbstractThemeableEditor) parentElement).getFileService().getParseResult();
			if (rootNode != null)
			{
				return filter(rootNode.getChildren());
			}
		}
		else if (parentElement instanceof IParseNode)
		{
			return filter(((IParseNode) parentElement).getChildren());
		}
		else if (parentElement instanceof CommonOutlineItem)
		{
			// delegates to the parse node it references to
			return getChildren(((CommonOutlineItem) parentElement).getReferenceNode());
		}
		return EMPTY;
	}

	public Object getParent(Object element)
	{
		if (element instanceof IParseNode)
		{
			return ((IParseNode) element).getParent();
		}
		if (element instanceof CommonOutlineItem)
		{
			IParseNode node = ((CommonOutlineItem) element).getReferenceNode();
			return getOutlineItem(node.getParent());
		}
		return null;
	}

	public boolean hasChildren(Object element)
	{
		return getChildren(element).length > 0;
	}

	public Object[] getElements(Object inputElement)
	{
		return getChildren(inputElement);
	}

	public void dispose()
	{
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		boolean isCU = (newInput instanceof AbstractThemeableEditor);

		if (isCU && fListener == null)
		{
			final AbstractThemeableEditor editor = (AbstractThemeableEditor) newInput;
			fListener = new IParseListener()
			{
				public void parseCompletedSuccessfully()
				{
					Display.getDefault().asyncExec(new Runnable()
					{

						public void run()
						{
							if (!editor.hasOutlinePageCreated())
							{
								return;
							}

							// FIXME What if the parse failed! We don't really want to wipe the existing results! This
							// is just a hack!
							IParseNode node = editor.getFileService().getParseResult();
							if (node != null)
							{
								CommonOutlinePage page = editor.getOutlinePage();
								page.refresh();
								if (!autoExpanded)
								{
									page.expandToLevel(2);
									autoExpanded = true;
								}
							}
						}
					});
				}

				public void beforeParse(IParseState parseState)
				{
				}

				public void afterParse(IParseState parseState)
				{
				}
			};
			editor.getFileService().addListener(fListener);
			this.resolver = PathResolverProvider.getResolver(editor.getEditorInput());
		}
		else if (!isCU && fListener != null)
		{
			AbstractThemeableEditor editor = (AbstractThemeableEditor) oldInput;
			editor.getFileService().removeListener(fListener);
			autoExpanded = false;
			fListener = null;
			this.resolver = PathResolverProvider.getResolver(editor.getEditorInput());
		}
	}

	/**
	 * Subclass could override to return a specific list from the result.
	 * 
	 * @param nodes
	 *            the array containing the parse result
	 * @return the specific top level objects to display
	 */
	protected Object[] filter(IParseNode[] nodes)
	{
		return nodes;
	}
}
