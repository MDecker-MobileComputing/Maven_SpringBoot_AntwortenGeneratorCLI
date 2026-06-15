package de.eldecker.dhbw.spring.antwortengenerator.engine;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


/**
 * Diese Bean-Klasse beinhaltet die eigentliche KI-Funktionalität.
 */
@Service
public class KIChatService {
	
	/** Anzahl der falschen Antworten (Distraktoren), die für jede Frage erzeugt werden sollen. */
	private final int ANZAHL_FALSCHE_ANTWORTEN = 5;
	
	/** Gesamtanzahl der Antworten, die für einen Request von KI zurückgegeben werden soll. */ 
	private final int ANZAHL_ANTWORTEN_GESAMT = ANZAHL_FALSCHE_ANTWORTEN + 1; 
	
	/** 
	 * Option-Builder-Objekt, um in Datei {@code application.properties} gesetzte Gemini-Parameter
	 * (Modell-Name und Temperatur) zu überschreiben. 
	 */
	@SuppressWarnings("unused")
	private static final Builder BUILDER_CUSTOM_OPTIONS = 
							GoogleGenAiChatOptions.builder()
							                      .model( "gemini-3.1-pro-preview" )
							                      .temperature( 0.9 );	
	
    /** 
     * Vorlage für Prompt.
     * Die Anzahlwerte werden im Konstruktor ersetzt, die Frage am Ende wird für jeden
     * Aufruf individuell ersetzt.  
     */
    private String _promptTemplate  = 
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
    private final ObjectMapper _objectMapper = new ObjectMapper();
    
    
    /**
     * Konstruktor: Zentrales API-Objekt erzeugen und in Prompt-Template
     * Zahlenwerte ersetzen.  
     */
    @Autowired
    public KIChatService( ChatClient.Builder geminiChatClientBuilder ) {
    	
    	_chatClient = geminiChatClientBuilder.build();


    	final String anzGesamtStr = ANZAHL_ANTWORTEN_GESAMT  + "";
    	final String anzFalschStr = ANZAHL_FALSCHE_ANTWORTEN + "";
    	
		_promptTemplate =
			_promptTemplate.replace( "{{ANZAHL_GESAMT}}", anzGesamtStr )
			               .replace( "{{ANZAHL_FALSCH}}", anzFalschStr );
    }
    

	/**
	 * Antwortoptionen für {@code singleChoiceFrage} von KI erzeugen lassen.
	 * <br><br>
	 * 
	 * Abfrage verbrauchte Token wäre auch möglich:
	 * https://gist.github.com/MDecker-MobileComputing/874c8f6efae3e37e6873b09abea8672a
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

		final String prompt = 
				_promptTemplate.replace( "{{FRAGE}}", singleChoiceFrage ); 

    	try {
    		    		    	
			final String antwortVonKiString = _chatClient.prompt() 										
												         .user( prompt )
												         //.options( BUILDER_CUSTOM_OPTIONS )
												         .call()
												         .content();

			final AntwortOptionenRecord antwortOptionen = 
					_objectMapper.readValue( antwortVonKiString, 
							                 AntwortOptionenRecord.class );

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

			final String[] ergebnisArray = new String[ falscheAntwortenListe.size() + 1 ];
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
