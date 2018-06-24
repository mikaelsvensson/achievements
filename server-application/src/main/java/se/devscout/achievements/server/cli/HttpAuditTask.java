package se.devscout.achievements.server.cli;

import com.google.common.collect.ImmutableMultimap;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.data.dao.AuditingDao;

import java.io.PrintWriter;

public class HttpAuditTask extends DatabaseTask {
    private final AuditingDao auditingDao;

    public HttpAuditTask(SessionFactory sessionFactory, AuditingDao auditingDao) {
        super("http-log", sessionFactory);
        this.auditingDao = auditingDao;
    }

    @Override
    protected void execute(ImmutableMultimap<String, String> parameters, PrintWriter output, Session session) throws Exception {
        // TODO: Make number of records to return configurable
        auditingDao.readLatest(10).forEach(record -> {
            output.printf("%-30s%-10s%-5s%s%n",
                    record.getDateTime(),
                    record.getHttpMethod(),
                    record.getResponseCode(),
                    record.getUser().getName());
        });
    }
}
