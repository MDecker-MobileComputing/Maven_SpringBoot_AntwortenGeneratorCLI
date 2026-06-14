package de.eldecker.dhbw.spring.antwortengenerator;

import java.util.List;


/**
 * Record für JSON-Antwort von KI.
 */
public record AntwortOptionenRecord( String       correct, 
		                             List<String> wrong 
		                           ) {
}
