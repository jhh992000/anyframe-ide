/*   
 * Copyright 2008-2011 the original author or authors.   
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
package org.anyframe.ide.eclipse.core.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;

/**
 * This is an MessageUtil class.
 * @author Changje Kim
 * @author Sooyeon Park
 */
public class MessageUtil {

    protected MessageUtil() {
        throw new UnsupportedOperationException(); // prevents calls from subclass
    }

    public static String getMessage(String key) {
        String message;
        try {
            message = ResourceBundle.getBundle("messages").getString(key);
            if (message == null) {
                message = key;
            }
        } catch (MissingResourceException e) {
            ExceptionUtil.showException(MessageUtil
                .getMessage("exception.log.resource"), IStatus.INFO, e);
            message = key;
        }

        return message;
    }

    public static String getMessage(String key, String test) {
        return getMessage(key);
    }
}
