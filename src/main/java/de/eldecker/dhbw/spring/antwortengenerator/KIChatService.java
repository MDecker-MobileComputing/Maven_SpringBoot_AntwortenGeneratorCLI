package de.eldecker.dhbw.spring.antwortengenerator;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


/**
 * Diese Bean-Klasse beinhaltet die eigentliche KI-Funktionalität.
 */
@Service
public class KIChatService {
	
    /** Vorlage für Prompt. */
    final static String PROMPT_TEMPLATE  = 
    		               """ 
							Du bist ein Generator für Single-Choice-Antwortoptionen.
							
							Aufgabe:
							Erzeuge zu der folgenden Frage genau {{ANZAHL_GESAMT}} Antwortoptionen:
							- 1 richtige Antwort
							- {{ANZAHL_FALSCH}} falsche, aber plausible Distraktoren
							
							Regeln:
							- Alle Antworten kurz halten (max. 12 Wörter).
							- Keine Nummerierung, keine Erklärungen, kein Markdown.
							- Alle Antworten müssen unterschiedlich sein.
							- Die falschen Antworten dürfen nicht offensichtlich absurd sein.
							- Antworte ausschließlich mit gültigem JSON nach diesem Schema:
							
							{
							  "correct": "string",
							  "wrong": ["string", "string", "..."]
							}
							
							Frage:
							{{FRAGE}}
     		               """;

	/** Zentrales API-Objekt für Kommunikation mit KI. */
    private final ChatClient _chatClient;

    /** Für Deserialisierung von JSON benötigt. */
    private ObjectMapper _objectMapper = new ObjectMapper();
    
    
    /**
     * Konstruktor für Erzeugung API-Objekt.
     */
    @Autowired
    public KIChatService( ChatClient.Builder geminiChatClientBuilder ) {
    	
    	_chatClient = geminiChatClientBuilder.build();
    }
    
       

	final int ANZAHL_FALSCHE_ANTWORTEN = 5;

	/**
	 * Antwortoptionen für {@code singleChoiceFrage} von KI erzeugen lassen.
	 *
	 * @param singleChoiceFrage Frage, für die Single-Choice-Antworten
	 *
	 * @param anzahlFalscheAntworten Anzahl gewünschter falscher Antworten
	 *
	 * @return Array mit Antworten; die erste (Index=0) ist die richtige,
	 *         danach folgen die falschen Antworten (Distraktoren)
	 *
	 * @throws AntwortenException Fehler bei Antwortenerzeugung
	 */
	public String[] antwortenErzeugen( String singleChoiceFrage  )
											throws AntwortenException {

    	
		final int anzahlGesamtAntworten = ANZAHL_FALSCHE_ANTWORTEN + 1;
		final String prompt =
				PROMPT_TEMPLATE.replace( "{{ANZAHL_GESAMT}}", Integer.toString( anzahlGesamtAntworten    ) )
				               .replace( "{{ANZAHL_FALSCH}}", Integer.toString( ANZAHL_FALSCHE_ANTWORTEN ) )
				               .replace( "{{FRAGE}}", singleChoiceFrage );
    	try {
    	
			final String antwortVonKiString = 
					_chatClient.prompt()
							.user( prompt )
							.call()
							.content();

			final AntwortOptionenRecord antwortOptionen = 
					_objectMapper.readValue( antwortVonKiString, AntwortOptionenRecord.class );

			final List<String> falscheAntwortenListe = antwortOptionen.wrong();
			if ( falscheAntwortenListe == null ) {

				throw new AntwortenException( "JSON-Antwort enthält kein Feld 'wrong'." );
			}
			
			if ( falscheAntwortenListe.size() != ANZAHL_FALSCHE_ANTWORTEN ) {
				
				final String warnText = 
						String.format( 
								"%d statt %d falsche Antworten von KI erhalten.", 
								falscheAntwortenListe.size(),
								ANZAHL_FALSCHE_ANTWORTEN );
				System.out.println( warnText );
			}

			final String[] ergebnisArray = new String[ anzahlGesamtAntworten ];
			ergebnisArray[ 0 ] = antwortOptionen.correct();
			for ( int i = 0; i < falscheAntwortenListe.size(); i++ ) {

				ergebnisArray[ i + 1 ] = falscheAntwortenListe.get( i );
			}

			return ergebnisArray;
    	}
    	catch ( JacksonException ex ) {
    		
    		throw new AntwortenException(
    				"Fehler beim Parsen der JSON-Antwort von der KI: " + ex.getMessage() );
    	}
    }
}
