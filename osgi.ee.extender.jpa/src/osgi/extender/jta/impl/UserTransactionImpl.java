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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * User transaction implementation. Simple component to delegate things to 
 * the transaction manager service.  
 */
@Component
public class UserTransactionImpl implements UserTransaction {
    private TransactionManager manager;
    
    @Override
    public void begin() throws NotSupportedException, SystemException {
        if (manager.getStatus() == Status.STATUS_NO_TRANSACTION) {
            manager.begin();
        }
    }

    @Override
    public void commit() throws HeuristicMixedException,
            HeuristicRollbackException, IllegalStateException,
            RollbackException, SecurityException, SystemException {
        manager.commit();
    }

    @Override
    public int getStatus() throws SystemException {
        return manager.getStatus();
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException,
            SystemException {
        manager.rollback();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        manager.setRollbackOnly();
    }

    @Override
    public void setTransactionTimeout(int t) throws SystemException {
        manager.setTransactionTimeout(t);
    }

    @Reference
    void setTransactionManager(TransactionManager manager) {
        this.manager = manager;
    }
}
