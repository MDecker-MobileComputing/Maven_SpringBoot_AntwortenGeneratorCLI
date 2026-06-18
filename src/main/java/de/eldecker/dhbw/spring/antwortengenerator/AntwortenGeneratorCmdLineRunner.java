package de.eldecker.dhbw.spring.antwortengenerator;

import java.util.Optional;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import de.eldecker.dhbw.spring.antwortengenerator.engine.AntwortenException;
import de.eldecker.dhbw.spring.antwortengenerator.engine.KIChatService;


/**
 * Klasse mit interaktiver Text-UI.  
 */
@Component
public class AntwortenGeneratorCmdLineRunner implements CommandLineRunner {

    /** Text-Scanner für Einlesen Nutzereingabe (Frage) von Tastatur. */
    final Scanner _scanner = new Scanner( System.in );
    
    /** Bean, die eigentliche KI-Anfrage durchführt. */
    private KIChatService _kiChatService;
    
    
    /**
     * Konstruktor für Dependency Injection.
     */
    @Autowired
    public AntwortenGeneratorCmdLineRunner( KIChatService aiChatService ) {
    	
    	_kiChatService = aiChatService;
    }
	
    
    /**
     * Diese Methode wird ausgeführt sobald sich die Spring-Boot-Anwendung
     * initialisiert hat.
     * 
     * @param args wird nicht ausgewertet
     */
	@Override
	public void run( String... args ) throws Exception {
		
		while ( true ) {
			
			final Optional<String> frageOptional = frageEinlesen();
			if ( frageOptional.isEmpty() ) {
				
				System.out.println( "\nKein Frage eingegeben, beende Programm.\n" );
				break;
			}
			
			final String frage = frageOptional.get();
			try {
				
				final String antwortenArray[] = 
						_kiChatService.antwortenErzeugen( frage );
				
				System.out.println( "\tRichtige Antwort: " + antwortenArray[0] );

				System.out.println( "\tFalsche Antworten: " );
				for ( int i = 1; i < antwortenArray.length; i++ ) {
				
					System.out.println( "\t\t" + antwortenArray[i] );
				}
			}
			catch ( AntwortenException ex ) {
				
				System.out.println( "Fehler: " +ex.getMessage() );
			}			
		}		
	}

	/**
	 * Single-Choice-Frage von Tastatur einlesen.
	 * 
	 * @return Optional enthält Fragetext wenn von Nutzer eingegeben,
	 *         sonst leer
	 */
    private Optional<String> frageEinlesen() {

        System.out.print ( "\nSingle-Choice-Frage eingeben (leer für Programmende) > " );
        String nutzereingabeString1 = _scanner.nextLine();

        nutzereingabeString1 = nutzereingabeString1.trim();
        if ( nutzereingabeString1.isBlank() ) {

            return Optional.empty();

        } else {

            return Optional.of( nutzereingabeString1 );
        }
    }	
}
