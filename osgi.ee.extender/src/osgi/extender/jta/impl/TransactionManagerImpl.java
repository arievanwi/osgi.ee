/*
 * Copyright 2015, Imtech Traffic & Infra
 * Copyright 2015, aVineas IT Consulting
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
package osgi.extender.jta.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * Transaction manager implementation. Very limited, but works in the case of
 * resource local transactions or JTA over multiple database connections (sort of).
 */
class TransactionManagerImpl implements TransactionManager {
	private long DEFAULTTIMEOUT = 10000L;
	private Map<Thread, TransactionImpl> transactions = new HashMap<>();
	private Timer timer;
	
	TransactionManagerImpl() {
		setTransactionTimer(DEFAULTTIMEOUT);
	}
	
	@Override
	public void begin() {
		TransactionImpl impl = _getTransaction(true);
		impl.setStatus(Status.STATUS_ACTIVE);
	}

	private TransactionImpl remove() {
		synchronized (transactions) {
			return transactions.remove(Thread.currentThread());
		}
	}
	
	@Override
	public void commit() {
		_getTransaction(true).commit();
		remove();
	}

	@Override
	public int getStatus() {
		TransactionImpl trans = _getTransaction(true);
		return trans.getStatus();
	}

	private TransactionImpl _getTransaction(boolean force) {
		synchronized (transactions) {
			TransactionImpl impl = transactions.get(Thread.currentThread());
			if (impl == null && force) {
				transactions.put(Thread.currentThread(), impl = new TransactionImpl());
			}
			return impl;
		}
	}
	
	@Override
	public Transaction getTransaction() {
		return _getTransaction(true);
	}

	@Override
	public void resume(Transaction trans) throws SystemException {
		throw new SystemException("no transaction resuming is possible with this transaction manager");
	}

	@Override
	public void rollback() {
		_getTransaction(true).rollback();
		remove();
	}

	@Override
	public void setRollbackOnly() {
		_getTransaction(true).setRollbackOnly();
	}

	private void setTransactionTimer(final long timo) {
		timer = new Timer("TransactionTimeout");
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				long overdue = System.currentTimeMillis() - timo;
				synchronized (transactions) {
					List<Map.Entry<Thread, TransactionImpl>> overdues = transactions.entrySet().stream().
							filter((e) -> e.getValue().getStartTime() < overdue).
							collect(Collectors.toList());
					overdues.stream().forEach((e) -> {
						transactions.remove(e.getKey());
						e.getValue().rollback();
					});
				}
			}
		};
		timer.schedule(task, timo, timo);
	}
	
	@Override
	public void setTransactionTimeout(int timeout) throws SystemException {
		if (timer != null) {
			timer.cancel();
		}
		long tmo = 10000L;
		if (tmo < 0) {
			throw new SystemException("transaction timeout must be larger or equal 0");
		}
		else if (timeout > 0) {
			tmo = timeout * 1000;
		}
		setTransactionTimer(tmo);
	}

	@Override
	public Transaction suspend() throws SystemException {
		throw new SystemException("no transaction suspending is possible with this transaction manager");
	}
	
	void destroy() {
		timer.cancel();
	}
}