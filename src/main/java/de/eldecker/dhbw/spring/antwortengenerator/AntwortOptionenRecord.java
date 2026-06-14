package de.eldecker.dhbw.spring.antwortengenerator;

import java.util.List;


/**
 * Record für eine richtige und mehrere falsche Antworten, die von der KI zurückgeliefert
 * wurde.
 * 
 * @param correct Richtige Antwort
 * 
 * @param wrong Liste mit den falschen Antworten (Distraktoren)
 */
public record AntwortOptionenRecord( String       correct, 
		                             List<String> wrong 
		                           ) {
}
