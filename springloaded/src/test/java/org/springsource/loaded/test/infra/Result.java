/*
 * Copyright 2010-2012 VMware and contributors
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

package org.springsource.loaded.test.infra;

/**
 * Captures the result of executing a method: the return value from the method, the stdout and the stderr.
 * 
 * @author Andy Clement
 * @since 1.0
 */
public class Result implements IResult {

	public Object returnValue;

	public final String stdout;

	public final String stderr;

	public Result(Object returnValue, String stdout, String stderr) {
		this.returnValue = returnValue;
		if (stdout.endsWith("\n")) {
			stdout = stdout.substring(0, stdout.length() - 1);
		}
		if (stderr.endsWith("\n")) {
			stderr = stderr.substring(0, stderr.length() - 1);
		}
		this.stdout = stdout.trim();
		this.stderr = stderr.trim();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("============================\n");
		sb.append("ReturnValue was '" + returnValue + "'\n");
		sb.append("STDOUT was\n" + stdout + "\n");
		sb.append("STDERR was\n" + stderr + "\n");
		sb.append("============================\n");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((returnValue == null) ? 0 : returnValue.hashCode());
		result = prime * result + ((stderr == null) ? 0 : stderr.hashCode());
		result = prime * result + ((stdout == null) ? 0 : stdout.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Result other = (Result) obj;
		if (returnValue == null) {
			if (other.returnValue != null)
				return false;
		}
		else if (!returnValue.equals(other.returnValue))
			return false;
		if (stderr == null) {
			if (other.stderr != null)
				return false;
		}
		else if (!stderr.equals(other.stderr))
			return false;
		if (stdout == null) {
			if (other.stdout != null)
				return false;
		}
		else if (!stdout.equals(other.stdout))
			return false;
		return true;
	}

	public String getSummary() {
		return "" + returnValue;
	}

}
