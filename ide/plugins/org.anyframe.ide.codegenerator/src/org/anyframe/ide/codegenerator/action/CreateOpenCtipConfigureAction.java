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
package org.anyframe.ide.codegenerator.action;

import org.anyframe.ide.codegenerator.CodeGeneratorActivator;
import org.anyframe.ide.codegenerator.messages.Message;
import org.anyframe.ide.codegenerator.views.CtipView;
import org.eclipse.jface.viewers.TreeSelection;

/**
 * This is a createOpenCtipConfigureAction class.
 * 
 * @author junghwan.hong
 * @since 2.7.0
 * 
 */
public class CreateOpenCtipConfigureAction extends AbstractlViewAction {

	/**
	 * Constructor of CreateOpenCtipConfigureAction forward actionId,
	 * actionTooltipText, actionIconId to AbstractlViewAction
	 */
	public CreateOpenCtipConfigureAction() {
		super(Message.view_ctip_action_openctipconfig_title,
				Message.view_ctip_action_openctipconfig_tooltip,
				Message.image_configuration);
	}

	/**
	 * Run this Action with message box
	 */
	public void run() {
		// open pop up window
		CtipView view = CodeGeneratorActivator.getDefault().getCtipView();
		TreeSelection selection = (TreeSelection) view.getSelectedId();

		if (selection != null) {
			String ctipUrl = selection.getPaths()[0].getFirstSegment()
					.toString();

			ctipUrl = ctipUrl.substring(ctipUrl.toLowerCase()
					.indexOf("http://"));
			view.openCtipConfigurationPopup(ctipUrl);
		}
	}
}
