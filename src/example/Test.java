package example;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.EntityType;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.dynamic.DynamicHelper;
import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.jpa.JpaHelper;
import org.eclipse.persistence.jpa.dynamic.JPADynamicHelper;
import org.eclipse.persistence.jpa.dynamic.JPADynamicTypeBuilder;

import example.util.ExamplePropertiesLoader;

public class Test {

	public static void main(String[] args) {
		// Create a dynamic class loader and create the types.
		DynamicClassLoader dcl = new DynamicClassLoader(Thread.currentThread()
				.getContextClassLoader());
		Class<?> pacienteClass = dcl
				.createDynamicClass("br.com.innovatium.netmanager.Paciente");
		JPADynamicTypeBuilder pacienteBuilder = new JPADynamicTypeBuilder(
				pacienteClass, null, "paciente");
		pacienteBuilder.setPrimaryKeyFields("idpaciente");

		pacienteBuilder
				.addDirectMapping("id", int.class, "paciente.idpaciente");
		pacienteBuilder.addDirectMapping("nome", String.class, "paciente.nome");

		pacienteBuilder.configureSequencing("seq_paciente", "idpaciente");

		DynamicType pacienteType = pacienteBuilder.getType();

		// Create an entity manager factory.
		EntityManagerFactory emf = createEntityManagerFactory(dcl, "default");

		// Create JPA Dynamic Helper (with the emf above) and after the types
		// have been created and add the types through the helper.
		JPADynamicHelper helper = new JPADynamicHelper(emf);
		helper.addTypes(true, true, pacienteType);

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		DynamicEntity paciente = pacienteType.newDynamicEntity();

		paciente.set("nome", "vinicius fernandes");

		em.persist(paciente);

		DynamicEntity entity = (DynamicEntity) em.createQuery("select p from Paciente p where p.id = :id")
				.setParameter("id", 1).getSingleResult();

		em.getTransaction().commit();
		em.clear();
		
		System.out.println("O paciente eh: "+entity.get("nome"));
	}

	public static EntityManagerFactory createEntityManagerFactory(
			DynamicClassLoader dcl, String persistenceUnit) {
		Map<Object, Object> properties = new HashMap<Object, Object>();
		ExamplePropertiesLoader.loadProperties(properties);
		properties.put(PersistenceUnitProperties.CLASSLOADER, dcl);
		properties.put(PersistenceUnitProperties.WEAVING, "static");
		return Persistence.createEntityManagerFactory(persistenceUnit,
				properties);
	}

}
