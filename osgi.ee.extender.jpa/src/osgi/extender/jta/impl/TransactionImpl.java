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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.HeuristicMixedException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Transaction implementation. Simple variant that does what the standard says (I think).
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
    
    void setStatus(int status) {
        this.status = status;
    }
    
    @Override
    public boolean delistResource(XAResource res, int flag) {
        try {
            res.end(xid, (status == Status.STATUS_MARKED_ROLLBACK) ? XAResource.TMFAIL : XAResource.TMSUCCESS);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        return true;
    }

    @Override
    public boolean enlistResource(XAResource res) {
        resources.add(res);
        try {
            res.start(xid, 0);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        return true;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void registerSynchronization(Synchronization sync) {
        this.toSync.add(sync);
    }

    private static <T> void doWith(T t, Cons<T> cons) {
        try {
            cons.accept(t);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
    
    @Override
    public void rollback() throws SystemException {
        setStatus(Status.STATUS_ROLLING_BACK);
        try {
            toSync.stream().forEach((s) -> doWith(s, (ss) -> ss.beforeCompletion()));
            resources.stream().forEach((r) -> doWith(r, (rr) -> delistResource(rr, 0)));
            resources.stream().forEach((r) -> doWith(r, (rr) -> rr.rollback(xid)));
            toSync.stream().forEach((s) ->  doWith(s, (ss) -> ss.afterCompletion(Status.STATUS_ROLLEDBACK)));
        } catch (Exception exc) {
            exc.printStackTrace();
            throw new SystemException(exc.getMessage());
        }
    }
    
    @Override
    public void commit() throws HeuristicMixedException, SystemException {
        if (status == Status.STATUS_MARKED_ROLLBACK) {
            rollback();
        }
        else {
            setStatus(Status.STATUS_COMMITTING);
            try {
                toSync.stream().forEach((s) -> doWith(s, (ss) -> ss.beforeCompletion()));
                resources.stream().forEach((r) -> doWith(r, (rr) -> delistResource(rr, 0)));
                resources.stream().forEach((r) -> doWith(r, (rr) -> rr.prepare(xid)));
                resources.stream().forEach((r) -> doWith(r, (rr) -> rr.commit(xid, true)));
                toSync.stream().forEach((s) ->  doWith(s, (ss) -> ss.afterCompletion(Status.STATUS_COMMITTED)));
            } catch (Exception exc) {
                exc.printStackTrace();
                throw new HeuristicMixedException(exc.getMessage());
            }
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
        this.thisSequence = sequence++;
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
