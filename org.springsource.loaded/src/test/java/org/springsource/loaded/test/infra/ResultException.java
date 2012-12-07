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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;


/**
 * Captures the exception raised by executing a method: the exception the stdout and the stderr.
 * 
 * @author Kris De Volder
 * @since 1.0
 */
@SuppressWarnings("serial")
public class ResultException extends Exception implements IResult {
	public final Throwable exception;
	public final String stdout;
	public final String stderr;

	public ResultException(Throwable exception, String stdout, String stderr) {
		super(exception);
		this.exception = exception;
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
		sb.append("Exception was '" + exception.getClass() + "'\n");
		sb.append("Exception msg '" + exception.getMessage() + "'\n");
		sb.append(getStackTraceAsString());
		sb.append("STDOUT was\n" + stdout + "\n");
		sb.append("STDERR was\n" + stderr + "\n");
		sb.append("============================\n");
		return sb.toString();
	}

	public String getStackTraceAsString() {
		StringWriter stringWriter = new StringWriter();
		exception.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.toString();
	}

	public Throwable getDeepestCause() {
		Throwable cause = exception;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause;
	}

	private int getNestingLevel() {
		int nestingLevel = 0;
		Throwable cause = exception;
		while (cause.getCause() != null) {
			nestingLevel++;
			cause = cause.getCause();
		}
		return nestingLevel;
	}

	/**
	 * This equals method is used by {@link GenerativeSpringLoadedTest}s to determine whether the behavior observed on regular Java
	 * and SpringLoaded are accepted as sufficiently equivalent to pass the test.
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		//1) We are both ResultExpeptions!
		if (obj.getClass() != this.getClass()) {
			return false;
		}
		ResultException other = (ResultException) obj;

		//2) We are the same type of Exception
		if (other.exception.getClass() != this.exception.getClass()) {
			return false;
		}

		//3) We have the same nesting depth
		if (other.getNestingLevel() != this.getNestingLevel()) {
			return false;
		}

		//4) Deepest causes are of same type
		Throwable myCause = this.getDeepestCause();
		Throwable otherCause = other.getDeepestCause();
		if (myCause.getClass() != otherCause.getClass()) {
			return false;
		}

		//5) Deepest causes have same message
		if (myCause.getMessage() == null) {
			return otherCause.getMessage() == null;
		}
		return myCause.getMessage().equals(otherCause.getMessage());

	}

	@Override
	public int hashCode() {
		Throwable e = getDeepestCause();
		int hash = e.getClass().hashCode();
		return hash;
	}

	public String getSummary() {
		Throwable cause = getDeepestCause();
		return cause.getClass().getSimpleName() + " " + cause.getMessage();
	}

}