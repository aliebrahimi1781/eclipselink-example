/*******************************************************************************
 * Copyright (c) 2007, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *      dclarke - JPA Employee example using XML (bug 217884)
 ******************************************************************************/
package example;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaHelper;
import org.eclipse.persistence.jpa.dynamic.JPADynamicHelper;
import org.eclipse.persistence.queries.QueryByExamplePolicy;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;

/**
 * Simple query examples for the XML mapped Employee domain model.
 * 
 * @author dclarke
 * @since EclipseLink 1.1
 */
public class Queries {

    /**
     * Simple example using dynamic JP QL to retrieve all Employee instances
     * sorted by lastName and firstName.
     */
    public List<DynamicEntity> readAllEmployeesUsingJPQL(EntityManager em) {
        return em.createQuery("SELECT e FROM Employee e ORDER BY e.lastName ASC, e.firstName ASC", DynamicEntity.class).getResultList();
    }

    /**
     * In addition to supporting configuration of JOIN FETCH within the JPQL of
     * a query EclipseLink also supports configuration using query hints. This
     * can be useful as the same named query can be customized for different
     * scenarios in the application as well simplifying trying different query
     * optimisations within the code.
     */
    public List<DynamicEntity> joinFetchAndBatchHints(EntityManager em) {
        TypedQuery<DynamicEntity> query = em.createQuery("SELECT e FROM Employee e", DynamicEntity.class);
        query.setHint(QueryHints.FETCH, "e.address");
        query.setHint(QueryHints.BATCH, "e.phoneNumbers");

        List<DynamicEntity> emps = query.getResultList();

        System.out.println(">>> Query Executed... accessing phone numbers of first employee");

        emps.get(0).<Collection<DynamicEntity>> get("phoneNumbers").size();

        System.out.println(">>> Accessing join/batch fetched attributes for all employees ");
        for (DynamicEntity emp : emps) {
            System.out.println("\t> " + emp);
            System.out.println("\t\t " + emp.<DynamicEntity> get("address"));
            System.out.println("\t\t Phone: " + emp.<Collection<DynamicEntity>> get("phoneNumbers").size());
        }

        return emps;
    }

    /**
     * 
     * @param em
     * @return
     */
    public static int minimumEmployeeId(EntityManager em) {
        return ((Number) em.createQuery("SELECT MIN(e.id) FROM Employee e").getSingleResult()).intValue();
    }

    public static DynamicEntity minimumEmployee(EntityManager em) {
        return minimumEmployee(em, null);
    }

    public static DynamicEntity minimumEmployee(EntityManager em, Map<String, Object> hints) {
        TypedQuery<DynamicEntity> q = em.createQuery("SELECT e FROM Employee e WHERE e.id in (SELECT MIN(ee.id) FROM Employee ee)", DynamicEntity.class);
        if (hints != null) {
            for (Map.Entry<String, Object> entry : hints.entrySet()) {
                q.setHint(entry.getKey(), entry.getValue());
            }
        }
        return q.getSingleResult();
    }

    public List<DynamicEntity> findEmployeesUsingGenderIn(EntityManager em) {
        return em.createQuery("SELECT e FROM Employee e WHERE e.gender IN (:GENDER1, :GENDER2)", DynamicEntity.class).setParameter("GENDER1", "Male").setParameter("GENDER2", "Female").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<DynamicEntity> findUsingNativeReadAllQuery(EntityManager em) {
        JPADynamicHelper helper = new JPADynamicHelper(em);

        ReadAllQuery raq = new ReadAllQuery(helper.getType("Employee").getJavaClass());
        ExpressionBuilder eb = raq.getExpressionBuilder();
        raq.setSelectionCriteria(eb.get("gender").equal("Male"));

        Query query = JpaHelper.createQuery(raq, em);

        return query.getResultList();
    }

    /**
     * Example of EclipseLink's native query-by-example support.
     * 
     * @param em
     * @param sampleEmployee
     * @return
     */
    public DynamicEntity queryByExample(EntityManager em, DynamicEntity sampleEmployee) {
        QueryByExamplePolicy policy = new QueryByExamplePolicy();
        policy.excludeDefaultPrimitiveValues();
        ReadObjectQuery roq = new ReadObjectQuery(sampleEmployee, policy);

        // Wrap the native query in a JPA Query and execute it.
        Query query = JpaHelper.createQuery(roq, em);

        return (DynamicEntity) query.getSingleResult();
    }

    public static DynamicEntity minEmployeeWithAddressAndPhones(EntityManager em) {
        return em.createQuery("SELECT e FROM Employee e JOIN FETCH e.address WHERE e.id IN (SELECT MIN(p.ownerId) FROM PhoneNumber p)", DynamicEntity.class).getSingleResult();
    }

    public DynamicEntity minEmployeeWithManagerWithAddress(EntityManager em) {
        List<DynamicEntity> emps = em.createQuery("SELECT e FROM Employee e JOIN FETCH e.manager WHERE e.manager.address IS NOT NULL ORDER BY e.id", DynamicEntity.class).getResultList();
        if (emps != null && emps.size() >= 1) {
            return emps.get(0);
        }
        return null;
    }

    public static int minEmployeeIdWithAddressAndPhones(EntityManager em) {
        return ((Number) em.createQuery("SELECT e.id FROM Employee e JOIN FETCH e.address WHERE e.id IN (SELECT MIN(p.ownerId) FROM PhoneNumber p)").getSingleResult()).intValue();
    }

    /**
     * Example of using EclipseLink's casting in JPQL queries using TREAT AS.
     * This query will find all employees who have a LargeProject with a budget
     * greater then 1000 and at least one SmallProject with a name ending in
     * 'Reporter'.
     * 
     * @since EclipseLink 2.1
     */
    public List<DynamicEntity> findUsingTreatAs(EntityManager em) {
        return em.createQuery("SELECT DISTINCT(e) FROM Employee e JOIN TREAT(e.projects AS LargeProject) lp JOIN TREAT(e.projects AS SmallProject) sp WHERE lp.budget > 1000 AND sp.name LIKE '%Reporter'", DynamicEntity.class).getResultList();
    }

    /**
     * Example of using EclipseLink's casting in criteria queries using AS. This
     * query will find all employees who have a LargeProject with a budget
     * greater then 1000 and at least one SmallProject with a name ending in
     * 'Reporter'.
     * 
     * @since EclipseLink 2.1
     */
    @SuppressWarnings("unchecked")
    public List<DynamicEntity> findUsingTreatAsCriteria(EntityManager em) {
        JPADynamicHelper helper = new JPADynamicHelper(em);
        CriteriaBuilder qb = em.getCriteriaBuilder();

        CriteriaQuery<?> cq = qb.createQuery(helper.getType("Employee").getJavaClass()).distinct(true);
        Root<?> empRoot = cq.from(helper.getType("Employee").getJavaClass());

        Path<Double> largeProjectBudget = ((Path<?>) empRoot.join("projects").as(helper.getType("LargeProject").getJavaClass())).get("budget");
        Path<String> smallProjectName = ((Path<?>) empRoot.join("projects").as(helper.getType("SmallProject").getJavaClass())).get("name");

        cq.where(qb.and(qb.greaterThan(largeProjectBudget, 1000d), qb.like(smallProjectName, "%Reporter")));

        return (List<DynamicEntity>) em.createQuery(cq).getResultList();
    }
}