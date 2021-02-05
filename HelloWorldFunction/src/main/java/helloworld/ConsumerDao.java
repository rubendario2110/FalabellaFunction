package helloworld;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class ConsumerDao {

    public void save(Session session, Consumer  consumer) {
        Transaction transaction = session.beginTransaction();
        session.save(consumer);
        transaction.commit();
    }
}
