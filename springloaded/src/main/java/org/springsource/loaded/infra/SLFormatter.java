/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded.infra;

import java.util.logging.LogRecord;

/**
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class SLFormatter extends java.util.logging.Formatter {

	public String format(LogRecord record) {
		StringBuilder s = new StringBuilder();
		s.append(record.getLevel());
		String message = super.formatMessage(record);

		if (!(message.startsWith(">") || message.startsWith("<"))) {
			s.append(":");
			String sourceClassName = record.getSourceClassName();
			int idx;
			if ((idx = sourceClassName.lastIndexOf('.')) == -1) {
				s.append(record.getSourceClassName());
			}
			else {
				s.append(record.getSourceClassName().substring(idx + 1));
			}
			s.append(".");
			s.append(record.getSourceMethodName());
			s.append(":");
		}
		s.append(message);
		s.append("\n");
		return s.toString();
	}

}
