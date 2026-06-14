package de.eldecker.dhbw.spring.antwortengenerator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
							Erzeuge zu der folgenden Frage genau 4 Antwortoptionen:
							- 1 richtige Antwort
							- 3 falsche, aber plausible Distraktoren
							
							Regeln:
							- Alle Antworten kurz halten (max. 12 Wörter).
							- Keine Nummerierung, keine Erklärungen, kein Markdown.
							- Die 4 Antworten müssen unterschiedlich sein.
							- Die falschen Antworten dürfen nicht offensichtlich absurd sein.
							- Antworte ausschließlich mit gültigem JSON nach diesem Schema:
							
							{
							  "correct": "string",
							  "wrong": ["string", "string", "string"]
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
    
        
    /**
     * Antwortoptionen für {@code singleChoiceFrage} von KI erzeugen lassen.
     * 
     * @param singleChoiceFrage Frage, für die Single-Choice-Antworten 
     * 
     * @return Array mit vier Antworten; die erste (Index=0) ist die richtige,
     *         die anderen drei sind falsche Antworten (Distraktoren)
     * 
     * @throws AntwortenException Fehler bei Antwortenerzeugung
     */
    public String[] antwortenErzeugen( String singleChoiceFrage ) 
    											throws AntwortenException {
    	
    	final String prompt = PROMPT_TEMPLATE.replace( "{{FRAGE}}", singleChoiceFrage );
    	
    	final String antwortVonKiString = 
    			_chatClient.prompt()
    	                   .user( prompt )
    	           		   .call()
    	           		   .content();

    	final AntwortOptionenRecord antwortOptionen = 
    			_objectMapper.readValue( antwortVonKiString, AntwortOptionenRecord.class );
    	
        
    	return new String[]{ 
    			             antwortOptionen.correct(), 
    					     antwortOptionen.wrong().get(0),
    					     antwortOptionen.wrong().get(1),
    					     antwortOptionen.wrong().get(2),
    					   };
    }
}
