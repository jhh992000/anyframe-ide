/*
 * Copyright 2008-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.anyframe.ide.querymanager.build;

import org.anyframe.ide.querymanager.QueryManagerActivator;
import org.anyframe.ide.querymanager.messages.Message;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

/**
 * This is QMMarkerResolution class.
 * 
 * @author Surindhar.Kondoor
 * @author Sreejesh.Nair
 */
public class QMMarkerResolution implements IMarkerResolution {

	Location loc;

	public QMMarkerResolution(Location loc) {
		this.loc = loc;
	}

	public String getDescription() {
		return loc.getFile().getName();
	}

	public Image getImage() {
		return null;
	}

	public String getLabel() {
		return Message.build_queryidexist + loc.getFile().getName();
	}

	public void run(IMarker marker) {
		openEditor(loc.getFile());
		marker = createMarker(loc, IMarker.BOOKMARK, Message.build_duplicatequeryid_one
				+ loc.key + Message.build_queryidexistintheproject);
		IWorkbenchPage activePage1 = QueryManagerActivator.getDefault()
				.getActiveWorkbenchPage();
		if (activePage1.getActiveEditor() != null)
			IDE.gotoMarker(activePage1.getActiveEditor(), marker);

	}

	public void openEditor(IFile file) {

		// Open editor on new file.
		String editorId = "org.anyframe.editor.xml.MultiPageEditor";
		IWorkbenchWindow dw = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		try {
			if (dw != null) {
				IWorkbenchPage page = dw.getActivePage();
				if (page != null)
					page.openEditor(new FileEditorInput(file), editorId, true);

			}
		} catch (PartInitException exception) {
			return;
		}

	}

	private IMarker createMarker(Location loc, String type, String msg) {
		IMarker marker = null;
		try {
			marker = loc.file.createMarker(type);

			marker.setAttribute(IMarker.MESSAGE, msg);
			marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
			marker.setAttribute(IMarker.CHAR_START, loc.charStart);
			marker.setAttribute(IMarker.CHAR_END, loc.charEnd);
			// marker.setAttribute(IMarker., value)
			// marker.setAttribute(IMarker.LINE_NUMBER, 5);
			marker.setAttribute(IMarker.SEVERITY, IMarker.MESSAGE);
			// IDE.gotoMarker(activePage1.getActiveEditor(), marker);
		} catch (CoreException e) {
		}
		return marker;
	}
}
