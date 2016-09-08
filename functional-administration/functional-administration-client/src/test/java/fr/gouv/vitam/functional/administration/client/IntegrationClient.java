package fr.gouv.vitam.functional.administration.client;

import java.io.InputStream;

import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

public class IntegrationClient {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        AdminManagementClientRest client = new AdminManagementClientRest("localhost", 8082);
        InputStream stream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam-format-KO.xml");

        try {
            client.checkFormat(stream);;
            System.out.println("-------------------------------- check ok");
        } catch (ReferentialException e) {
            System.out.println("----------------------------------check NOT ok");
        }

    }

}
