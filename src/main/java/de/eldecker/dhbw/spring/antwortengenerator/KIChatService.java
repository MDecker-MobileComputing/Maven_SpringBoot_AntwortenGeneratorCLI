package de.eldecker.dhbw.spring.antwortengenerator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Diese Bean-Klasse beinhaltet die eigentliche KI-Funktionalität.
 */
@Service
public class KIChatService {

	/** Zentrales API-Objekt für Kommunikation mit KI. */
    private final ChatClient _chatClient;

    
    /**
     * Konstruktor für Erzeugung API-Objekt.
     */
    @Autowired
    public KIChatService( ChatClient.Builder geminiChatClientBuilder ) {
    	
    	_chatClient = geminiChatClientBuilder.build();
    }
    
    final String prompt1 = """ 
  		                   Gibt mir die richtige Antwort für die folgende Frage zurück. 
  		                   Die Antwort soll möglichst knapp sein, so dass sie als Antwortoption
  		                   für eine Single-Choice-Frage verwendet werden kann.
                           Die Frage lautet: 
     		               """;
    
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
    	
    	final String antwortRichtig = 
    			_chatClient.prompt()
    	                   .user( prompt1 + singleChoiceFrage )
    	           		   .call()
    	           		   .content();
        
    	return new String[]{ antwortRichtig };
    }
}
