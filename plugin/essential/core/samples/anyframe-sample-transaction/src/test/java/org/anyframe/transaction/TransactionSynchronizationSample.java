/*
 * Copyright 2008-2012 the original author or authors.
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
package org.anyframe.transaction;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * TransactionSynchronization sample class for transaction test
 * 
 * @author SoYon Lim
 * @author JongHoon Kim
 */
public class TransactionSynchronizationSample implements
		TransactionSynchronization {

	private int rollbackCount;

	private int commitCount;

	public void afterCommit() {
	}

	public void afterCompletion(int status) {
		switch (status) {
		case STATUS_COMMITTED:
			commitCount++;
			break;
		case STATUS_ROLLED_BACK:
			rollbackCount++;
			break;
		default:
			break;
		}
	}

	public void beforeCommit(boolean arg0) {

	}

	public void beforeCompletion() {

	}

	public void resume() {

	}

	public void suspend() {

	}

	public int getCommitCount() {
		return commitCount;
	}

	public int getRollbackCount() {
		return rollbackCount;
	}

	public void flush() {

	}

}
