package helloworld;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class ConsumerServices {
    private SessionFactory sessionFactory;
    private ConsumerDao consumerDao;

    public ConsumerServices(SessionFactory sessionFactory, ConsumerDao consumerDao) {
        this.sessionFactory = sessionFactory;
        this.consumerDao = consumerDao;
    }

    public int saveResponse(Consumer consumer) {
        try (Session session = sessionFactory.openSession()) {
            consumerDao.save(session, consumer);
            return consumer.getId();
        }
    }
}
