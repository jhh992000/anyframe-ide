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
package org.anyframe.ide.codegenerator.model.tree;

import org.anyframe.ide.codegenerator.messages.Message;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * This is an TreeLabelProvider class.
 * 
 * @author Changje Kim
 * @author Sooyeon Park
 */
public class TreeLabelProvider extends LabelProvider {
	private Image image = null;;

	public Image getImage(Object element) {
		if (((ITreeModel) element).getName().equals(
				Message.wizard_domain_init_message)
				|| ((ITreeModel) element).getElement() != null) {
			image = new Image(null, getClass().getResourceAsStream(
					Message.image_tables));
		} else {
			image = new Image(null, getClass().getResourceAsStream(
					Message.image_table));
		}
		return image;
	}

	public String getText(Object element) {
		return ((ITreeModel) element).getName();
	}

	public void dispose() {

	}

}
