package example;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.jpa.dynamic.JPADynamicHelper;
import org.eclipse.persistence.jpa.dynamic.JPADynamicTypeBuilder;

import example.util.ExamplePropertiesLoader;

public class Test {

	public static void main(String[] args) {
		testarMedpentient();
	}

	public static void testarMedpentient() {
		// Create a dynamic class loader and create the types.
		DynamicClassLoader dcl = new DynamicClassLoader(Thread.currentThread()
				.getContextClassLoader());
		Class<?> pacienteClass = dcl
				.createDynamicClass("br.com.innovatium.netmanager.Medpatient");

		JPADynamicTypeBuilder pacienteBuilder = new JPADynamicTypeBuilder(
				pacienteClass, null, "MEDPatient");
		pacienteBuilder.setPrimaryKeyFields("company", "patientid");
		pacienteBuilder.addDirectMapping("patientid", String.class,
				"MEDPatient.patientid");
		pacienteBuilder.addDirectMapping("company", String.class,
				"MEDPatient.company");
		pacienteBuilder.addDirectMapping("name", String.class,
				"MEDPatient.name");
		DynamicType pacienteType = pacienteBuilder.getType();

		// Create an entity manager factory.
		EntityManagerFactory emf = createEntityManagerFactory(dcl, "default");

		// Create JPA Dynamic Helper (with the emf above) and after the types
		// have been created and add the types through the helper.
		JPADynamicHelper helper = new JPADynamicHelper(emf);
		helper.addTypes(true, true, pacienteType);

		EntityManager em = emf.createEntityManager();
		java.util.Date start = new java.util.Date();
		List l = em
				.createQuery(
						"select p from Medpatient p where p.patientid like :patientid")
				.setParameter("patientid", 11111 + "%").getResultList();
		System.out.println("Tempo de pesquisa de pacientes "
				+ (new java.util.Date().getTime() - start.getTime()) + "(ms)");

	}

	public static void testarPaciente() {
		// Create a dynamic class loader and create the types.
		DynamicClassLoader dcl = new DynamicClassLoader(Thread.currentThread()
				.getContextClassLoader());
		Class<?> pacienteClass = dcl
				.createDynamicClass("br.com.innovatium.netmanager.Paciente");

		JPADynamicTypeBuilder pacienteBuilder = new JPADynamicTypeBuilder(
				pacienteClass, null, "paciente");
		pacienteBuilder.setPrimaryKeyFields("idpaciente", "codempresa");
		pacienteBuilder
				.addDirectMapping("id", int.class, "paciente.idpaciente");
		pacienteBuilder.addDirectMapping("empresa", int.class,
				"paciente.codempresa");
		pacienteBuilder.addDirectMapping("nome", String.class, "paciente.nome");
		pacienteBuilder.addDirectMapping("nascimento", Date.class,
				"paciente.nasc");
		pacienteBuilder.addDirectMapping("obito", Timestamp.class,
				"paciente.obito");

		// pacienteBuilder.configureSequencing("seq_paciente", "idpaciente");
		// pacienteBuilder.configureSequencing("seq_paciente_empresa",
		// "codempresa");

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

		paciente.set("nome", "mascos valerio");
		paciente.set("nascimento", new Date(new java.util.Date().getTime()));
		paciente.set("obito", new Timestamp(new java.util.Date().getTime()));

		em.persist(paciente);

		paciente = pacienteType.newDynamicEntity();
		paciente.set("empresa", 0);
		paciente.set("id", 1);
		paciente.set("nome", "vinicius");
		paciente.set("nascimento", new Date(new java.util.Date().getTime()));
		paciente.set("obito", new Timestamp(new java.util.Date().getTime()));
		em.persist(paciente);

		paciente = pacienteType.newDynamicEntity();
		paciente.set("empresa", 1);
		paciente.set("id", 1);
		paciente.set("nome", "renato");
		paciente.set("nascimento", new Date(new java.util.Date().getTime()));
		paciente.set("obito", new Timestamp(new java.util.Date().getTime()));
		em.persist(paciente);

		em.getTransaction().commit();
		em.clear();
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

	public static void pesquisarPaciente(int id) {
		DynamicClassLoader dcl = new DynamicClassLoader(Thread.currentThread()
				.getContextClassLoader());

		// Create an entity manager factory.
		EntityManagerFactory emf = createEntityManagerFactory(dcl, "default");
		EntityManager em = emf.createEntityManager();

		DynamicEntity entity = (DynamicEntity) em
				.createQuery("select p from Paciente p where p.id = :id")
				.setParameter("id", 0).getSingleResult();
		System.out.println("O paciente eh: " + entity.get("nome"));

	}

}
