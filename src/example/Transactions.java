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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.eclipse.persistence.config.PessimisticLock;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.jpa.dynamic.JPADynamicHelper;
import org.eclipse.persistence.queries.AttributeGroup;
import org.eclipse.persistence.sessions.CopyGroup;
import org.eclipse.persistence.sessions.server.Server;

public class Transactions {

    /**
     * New entities with new related related entities can be persisted using
     * <code>EntityManager.persist(newEntity)</code>. The cascade setting on the
     * mappings determine how the related entities are handled. In this case
     * Employee has its relationship to Address and PhoneNumber configured with
     * cascade-all so the associated new entities will also be persisted.
     */
    public DynamicEntity createUsingPersist(EntityManager em) {
        JPADynamicHelper helper = new JPADynamicHelper(em);

        DynamicEntity emp = helper.newDynamicEntity("Employee");
        emp.set("firstName", "Sample");
        emp.set("lastName", "Employee");
        emp.set("gender", "Male");
        emp.set("salary", 123456);

        DynamicEntity address = helper.newDynamicEntity("Address");
        emp.set("address", address);

        DynamicEntity phone = helper.newDynamicEntity("PhoneNumber");
        phone.set("type", "Mobile");
        phone.set("areaCode", "613");
        phone.set("number", "5551212");
        emp.<Collection<DynamicEntity>> get("phoneNumbers").add(phone);
        phone.set("owner", emp);

        em.persist(emp);

        em.flush();

        return emp;
    }

    /**
	 * 
	 */
    public DynamicEntity createUsingMerge(EntityManager em) {
        JPADynamicHelper helper = new JPADynamicHelper(em);

        DynamicEntity emp = helper.newDynamicEntity("Employee");
        emp.set("firstName", "Sample");
        emp.set("lastName", "Employee");
        emp.set("gender", "Male");
        emp.set("salary", 123456);

        DynamicEntity address = helper.newDynamicEntity("Address");
        emp.set("address", address);

        DynamicEntity phone = helper.newDynamicEntity("PhoneNumber");
        phone.set("type", "Mobile");
        phone.set("areaCode", "613");
        phone.set("number", "5551212");
        emp.<Collection<DynamicEntity>> get("phoneNumbers").add(phone);
        phone.set("owner", emp);

        // When merging the managed instance is returned from the call.
        // Further usage within the transaction must be done with this managed
        // entity.
        emp = em.merge(emp);

        em.flush();

        return emp;
    }

    /**
     * 
     * @param em
     * @throws Exception
     */
    public void pessimisticLocking(EntityManager em) throws Exception {

        // Find the Employee with the minimum ID
        int minId = Queries.minimumEmployeeId(em);

        em.getTransaction().begin();

        // Lock Employee using query with hint
        DynamicEntity emp = em.createQuery("SELECT e FROM Employee e WHERE e.id = :ID", DynamicEntity.class).setParameter("ID", minId).setHint(QueryHints.PESSIMISTIC_LOCK, PessimisticLock.Lock).getSingleResult();

        emp.set("salary", emp.<Integer> get("salary") - 1);

        em.flush();
    }

    /**
     * This example illustrates the use of a query returning an entity and data
     * from a related entity within a transaction. The returned entities are
     * managed and thus any changes are reflected in the database upon flush.
     * 
     * @param em
     * @throws Exception
     */
    public void updateEmployeeWithCity(EntityManager em) throws Exception {
        em.getTransaction().begin();

        @SuppressWarnings("unchecked")
        List<Object[]> emps = em.createQuery("SELECT e, e.address.city FROM Employee e").getResultList();
        DynamicEntity emp = (DynamicEntity) emps.get(0)[0];
        emp.set("salary", emp.<Integer> get("salary") + 1);

        em.flush();

        em.getTransaction().rollback();
    }

    /**
     * TODO
     */
    public DynamicEntity copyGroupMerge(EntityManager em) {
        JPADynamicHelper helper = new JPADynamicHelper(em);

        // Search for an Employee with an Address and Phone Numbers
        TypedQuery<DynamicEntity> query = em.createQuery("SELECT e FROM Employee e WHERE e.id IN (SELECT MIN(ee.id) FROM Employee ee)", DynamicEntity.class);

        DynamicEntity emp = query.getSingleResult();

        System.out.println(">>> Employee retrieved");

        // Copy only its names, address, and phone numbers
        AttributeGroup group = new CopyGroup();
        group.addAttribute("firstName");
        group.addAttribute("lastName");
        group.addAttribute("salary");
        group.addAttribute("address");

        AttributeGroup phonesGroup = em.unwrap(Server.class).getClassDescriptorForAlias("PhoneNumber").getFetchGroupManager().createFullFetchGroup();
        group.addAttribute("phoneNumbers", phonesGroup);

        DynamicEntity empCopy = (DynamicEntity) em.unwrap(JpaEntityManager.class).copy(emp, group);
        System.out.println(">>> Employee copied");

        // Modify the detached Employee inverting the names, address.city, and
        // adding a phone
        // number, and setting the salary
        empCopy.set("firstName", emp.<String> get("lastName"));
        empCopy.set("lastName", emp.<String> get("firstName"));
        empCopy.<DynamicEntity> get("address").set("city", emp.<DynamicEntity> get("address").<String> get("city") + "_");
        empCopy.set("salary", 1.0);

        DynamicEntity phone = helper.newDynamicEntity("PhoneNumber");
        phone.set("type", "TEST");
        phone.set("areaCode", "999");
        phone.set("number", "9999999");
        empCopy.<Collection<DynamicEntity>> get("phoneNumbers").add(phone);
        phone.set("owner", empCopy);

        // Merge the detached employee
        em.merge(empCopy);
        System.out.println(">>> Sparse merge complete");

        // Flush the changes to the database
        em.flush();

        System.out.println(">>> Flush complete");

        return emp;
    }

    /**
     * Create a new Employee entity by cloning an existing one using CopyGroup
     * with {@link JpaEntityManager#copy(Object, AttributeGroup)}
     */
    public DynamicEntity cloneEmployeeUsingCopy(EntityManager em) {
        // Search for an Employee with an Address and Phone Numbers
        TypedQuery<DynamicEntity> query = em.createQuery("SELECT e FROM Employee e WHERE e.id IN (SELECT MIN(ee.id) FROM Employee ee)", DynamicEntity.class);

        DynamicEntity emp = query.getSingleResult();

        System.out.println(">>> Employee retrieved");

        // Copy only its names and addrtess
        CopyGroup group = new CopyGroup();
        group.setShouldResetPrimaryKey(true);
        group.setShouldResetVersion(true);
        group.cascadeTree(); // use the attributes specified
        // Add attributes using canonical metamodel
        group.addAttribute("firstName");
        group.addAttribute("lastName");
        group.addAttribute("gender");
        group.addAttribute("salary");
        group.addAttribute("startTime");
        group.addAttribute("endTime");
        group.addAttribute("period");
        group.addAttribute("responsibilities");
        group.addAttribute("address");

        DynamicEntity empCopy = (DynamicEntity) em.unwrap(JpaEntityManager.class).copy(emp, group);

        em.persist(empCopy);

        em.flush();

        return empCopy;
    }
}