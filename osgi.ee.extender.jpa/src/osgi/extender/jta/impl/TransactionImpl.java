/*
 * Copyright 2015, Imtech Traffic & Infra
 * Copyright 2015, aVineas IT Consulting
 * Copyright 2015, Fujifilm Europe B.V.
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.persistence.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Transaction implementation. Simple variant that does what the standard says
 * (I think). Note that a transaction instance is thread safe because it is only
 * accessed from one thread, as required by the JEE standard.
 *
 * @author Arie van Wijngaarden
 */
public class TransactionImpl implements Transaction {
    interface Cons<T> {
        void accept(T t) throws Exception;
    }
    private int status = Status.STATUS_NO_TRANSACTION;
    private List<Synchronization> toSync = new ArrayList<>();
    private List<XAResource> resources = new ArrayList<>();
    private Xid xid = new XXid();
    private long startTime = System.currentTimeMillis();
    private Consumer<TransactionImpl> endNotifier;

    TransactionImpl(Consumer<TransactionImpl> end) {
    	this.endNotifier = end;
    }
    
    void setStatus(int status) {
        this.status = status;
    }

    @Override
    public boolean delistResource(XAResource res, int flag) {
        if (status == Status.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException("no transaction active. Cannot delist a resource");
        }
        try {
            res.end(xid, status == Status.STATUS_MARKED_ROLLBACK ? XAResource.TMFAIL : XAResource.TMSUCCESS);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        return true;
    }

    @Override
    public boolean enlistResource(XAResource res) {
        if (status == Status.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException("no transaction active. Cannot enlist a resource");
        }
        try {
            res.start(xid, 0);
            resources.add(res);
        } catch (Exception exc) {
            exc.printStackTrace();
            setStatus(Status.STATUS_MARKED_ROLLBACK);
            return false;
        }
        return true;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void registerSynchronization(Synchronization sync) {
        if (status == Status.STATUS_NO_TRANSACTION)
            throw new IllegalStateException("no transaction active. Cannot add a synchronization object");
        toSync.add(sync);
    }

    /**
     * Perform an action on a specific object. Catch exceptions on the fly and
     * handle according to the passed parameter.
     * 
     * @param t The object to pass to the consumer
     * @param cons The consumer
     * @param throwExceptionOnFail Indication whether to throw a runtime/other
     * exception in case of failure
     */
    private static <T> void doWith(T t, Cons<T> cons, boolean throwExceptionOnFail) {
        try {
            cons.accept(t);
        } catch (Exception exc) {
            if (throwExceptionOnFail) {
                throw new RuntimeException(exc);
            }
            exc.printStackTrace();
        }
    }

    /**
     * Experience shows that in some cases the synchronization objects are added
     * while we are doing the commits as a result of EntityListeners for
     * example. Therefore prevent concurrent modification exceptions.
     * 
     * @param c The consumer to execute on all synchronization objects
     */
    private void doWithSyncs(Consumer<Synchronization> c) {
        if (toSync.size() == 0) return;
        List<Synchronization> toDo = toSync;
        toSync = new ArrayList<>();
        try {
            // Perform the consumer on the existing list.
            toDo.stream().forEach((s) -> c.accept(s));
            // And on the new list recursively.
            doWithSyncs(c);
        } finally {
            // Gather the lot to the new list.
            toDo.addAll(toSync);
            toSync = toDo;
        }
    }
    
    private void _rollback() {
        if (status == Status.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException("no transaction active");
        }
        setStatus(Status.STATUS_ROLLING_BACK);
        resources.stream().forEach((r) -> doWith(r, (rr) -> delistResource(rr, 0), false));
        resources.stream().forEach((r) -> doWith(r, (rr) -> rr.rollback(xid), false));
        doWithSyncs((s) -> doWith(s, (ss) -> ss.afterCompletion(Status.STATUS_ROLLEDBACK), false));
        setStatus(Status.STATUS_ROLLEDBACK);
    }
    
    @Override
    public void rollback() {
    	try {
    		_rollback();
    	} finally {
    		endNotifier.accept(this);
    	}
    }

    @Override
    public void commit() {
    	try {
	        if (status == Status.STATUS_NO_TRANSACTION) {
	            throw new IllegalStateException("no transaction active");
	        }
	        if (status == Status.STATUS_MARKED_ROLLBACK) {
	            _rollback();
	        }
	        else if (status == Status.STATUS_ACTIVE) {
	            try {
	                doWithSyncs((s) -> doWith(s, (ss) -> ss.beforeCompletion(), true));
	                if (getStatus() == Status.STATUS_MARKED_ROLLBACK) {
	                    _rollback();
	                }
	                else {
	                    resources.stream().forEach((r) -> doWith(r, (rr) -> delistResource(rr, 0), true));
	                    setStatus(Status.STATUS_PREPARING);
	                    resources.stream().forEach((r) -> doWith(r, (rr) -> rr.prepare(xid), true));
	                    setStatus(Status.STATUS_COMMITTING);
	                    resources.stream().forEach((r) -> doWith(r, (rr) -> rr.commit(xid, true), true));
	                    doWithSyncs((s) -> doWith(s, (ss) -> ss.afterCompletion(Status.STATUS_COMMITTED), true));
	                    setStatus(Status.STATUS_COMMITTED);
	                }
	            } catch (Exception exc) {
	                _rollback();
	                throw new RollbackException("could not commit transaction. Rolled it back", exc);
	            }
	        }
	        else {
	            throw new IllegalStateException("transaction status " + status + " does not allow commit");
	        }
    	} finally {
        	endNotifier.accept(this);
    	}
    }

    @Override
    public void setRollbackOnly() {
        setStatus(Status.STATUS_MARKED_ROLLBACK);
    }

    long getStartTime() {
        return startTime;
    }
}

class XXid implements Xid {
    private static int sequence = 1;

    private int thisSequence;

    XXid() {
        thisSequence = sequence++;
    }

    @Override
    public byte[] getBranchQualifier() {
        return new byte[0];
    }

    @Override
    public int getFormatId() {
        return 29;
    }

    @Override
    public byte[] getGlobalTransactionId() {
        return ByteBuffer.allocate(8).putInt(thisSequence).array();
    }
}
