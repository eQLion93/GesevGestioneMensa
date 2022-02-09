package it.gesev.dao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import it.gesev.entities.Derrata;
import it.gesev.entities.TipoDerrata;
import it.gesev.enums.ColonneDerrataEnum;
import it.gesev.exc.GesevException;
import it.gesev.repository.DerrataRepository;
import it.gesev.repository.TipoDerrateRepositroy;

@Repository
@Component
public class DerrataDAOImpl implements DerrataDAO 
{
	private final Logger logger = LoggerFactory.getLogger(DerrataDAO.class);
	
	@Autowired
	DerrataRepository derrataRepository;
	
	@Autowired
	TipoDerrateRepositroy tipoDerrateRepositroy;
	
	@PersistenceContext
	EntityManager entityManager;
	
	@Value("${gesev.data.format}")
	private String dateFormat;
	
	/* Cerca tutte le derrata */
	@Override
	public List<Derrata> getAllDerrata(long tipoDerrataId) 
	{
		logger.info("Rierca di tutte le Derrate presenti sul database");
		return derrataRepository.findAllByDerrataId(tipoDerrataId);
	}

	/* Crea la Derrata */
	@Override
	public Long creaDerrata(Derrata derrata, int codiceTipoDerrata) 
	{
		logger.info("Creazione nuova derrata");
		
		if(StringUtils.isBlank(derrata.getDescrizioneDerrata()) || StringUtils.isBlank(derrata.getUnitaMisura())
				|| derrata.getPrezzo() <= 0 || derrata.getQuantitaMinima() <= 0)
		{
			logger.info("Impossibile creare una derrata. Campi inseriti non validi");
			throw new GesevException("Impossibile creare una derrata. Campi inseriti non validi", HttpStatus.BAD_REQUEST);
		}
		
		Optional<Derrata> optionalTipoDerrataDescrizione = derrataRepository.findByDescrizioneDerrata(derrata.getDescrizioneDerrata());
		if(optionalTipoDerrataDescrizione.isPresent())
			throw new GesevException("Descizione associata alla derrata gia' esistente", HttpStatus.BAD_REQUEST);
		
		Optional<TipoDerrata> optionalTipoDerrata = tipoDerrateRepositroy.findByCodice(codiceTipoDerrata);
		
		logger.info("Inserimento nuovo record derrata in corso...");
		derrata.setDataAggiornamentoGiacenza(new Date());
		Derrata derrataObj = derrata;
		//if(derrataObj == null)
		//	throw new GesevException("Impossibile inserire un nuovo reocrod nella tabella derrata", HttpStatus.INTERNAL_SERVER_ERROR);
		
		derrataObj.setDescrizioneDerrata(derrata.getDescrizioneDerrata());
		derrataObj.setUnitaMisura(derrata.getUnitaMisura());
		derrataObj.setPrezzo(derrata.getPrezzo());
		derrataObj.setGiacenza(derrata.getGiacenza());
		derrataObj.setDataAggiornamentoGiacenza(derrata.getDataAggiornamentoGiacenza());
		derrataObj.setQuantitaMinima(derrata.getQuantitaMinima());
		derrataObj.setCodiceMensa(derrata.getCodiceMensa());
		
		derrataObj.setDettaglioPrelevamento(derrata.getDettaglioPrelevamento());
		derrataObj.setDettaglioMovimento(derrata.getDettaglioMovimento());
		derrataObj.setTipoDerrata(optionalTipoDerrata.get());
		
		derrataRepository.save(derrataObj);
		
		return derrataObj.getDerrataId();
	}

	/* Cancellazione derrata */
	@Override
	public Long deleteDerrata(Long derrataId) 
	{
		logger.info("Accesso alla classe DerrataDAOImpl - Cancellazione deleteDerrata con ID " + derrataId);
		logger.info("Ricerca derrata con ID scpecificato...");
		Optional<Derrata> optionalDerrata = derrataRepository.findById(derrataId);
		if(!optionalDerrata.isPresent())
			throw new GesevException("Nessuna derrata trovato con l'ID specificato", HttpStatus.BAD_REQUEST);
		
		logger.info("Derrata trovata. Cancellazione in corso...");
		Derrata derrata = optionalDerrata.get();
		if(derrata.getDettaglioMovimento() != null)
			throw new GesevException("Impossibile cancellare la derrata, poiche' e' associata ad un dettaglio movimento", HttpStatus.BAD_REQUEST);
		
		derrataRepository.delete(derrata);
		return derrataId;
	}

	/* Aggiorna Derrata */
	@Override
	public Long aggiornaDerrata(Derrata derrata)
	{
		logger.info("Accesso alla classe DerrataDAOImpl metodo aggiornaDerrata");
		
		Integer maxCodice = derrataRepository.getMaxDerrataId();
		if(derrata.getDerrataId() > maxCodice || derrata.getDerrataId() < 0)
		{
			logger.info("Impossibile modificare la derrata, codice non presente");
			throw new GesevException("Impossibile modificare la derrata, codice non presente", HttpStatus.BAD_REQUEST);
		}
		
		if(derrata == null || StringUtils.isBlank(derrata.getDescrizioneDerrata()) || StringUtils.isBlank(derrata.getUnitaMisura())
				|| derrata.getPrezzo() <= 0 || derrata.getQuantitaMinima() <= 0)
			throw new GesevException("Impossibile aggiornare la derrata, dati non validi", HttpStatus.BAD_REQUEST);
		
		logger.info("Controllo dell'unicita' della descrizione fornita...");
		Optional<Derrata> optionalDerrata = derrataRepository.findByDescrizioneDerrata(derrata.getDescrizioneDerrata());
		if(optionalDerrata.isPresent() && optionalDerrata.get().getDescrizioneDerrata().equalsIgnoreCase(derrata.getDescrizioneDerrata()) && 
				optionalDerrata.get().getDerrataId() != derrata.getDerrataId())
			throw new GesevException("La descrizione fornita risulta gia' associata ad un altra derrata oppure codice non presente", HttpStatus.BAD_REQUEST);
		
		logger.info("Aggiornamento in corso...");
		Derrata derrataMom = null;
		if(optionalDerrata.isPresent() && optionalDerrata.get().getDerrataId() == derrata.getDerrataId())
			derrataMom = optionalDerrata.get();
		else
		{
			Optional<Derrata> derrataCercata = derrataRepository.findById(derrata.getDerrataId());
			if(derrataCercata.isPresent())
				derrataMom = derrataCercata.get();
		}
		
		if(derrataMom == null)
			throw new GesevException("Nessuna derrata presente con l'ID " + derrata.getDerrataId(), HttpStatus.BAD_REQUEST);
		
		derrataMom.setDescrizioneDerrata(derrata.getDescrizioneDerrata());
		derrataRepository.save(derrataMom);
		
		logger.info("Fine aggiornamento");
		return derrata.getDerrataId();
	}
	
	/* Derrata per un Lotto */
	@Override
	public List<Derrata> cercaTipoDerrataConColonna(String colonna, String value, long idLotto) 
	{
		logger.info("Ricerca della derrata sulla base della colonna " + colonna.toUpperCase() + " e del valore " + value);
		
		logger.info("Controllo esistenza colonna...");
		ColonneDerrataEnum colonnaEnum = null;
		
		try 
		{
			colonnaEnum = ColonneDerrataEnum.valueOf(colonna.toUpperCase());
		} 
		
		catch (Exception e) 
		{
			throw new GesevException("Si e' verificato un errore. " + ExceptionUtils.getStackFrames(e)[0], HttpStatus.BAD_REQUEST);
		}
		
		logger.info("Composizione della query di ricerca...");
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Derrata> criteriaQuery = criteriaBuilder.createQuery(Derrata.class);
		Root<Derrata> derrataRoot = criteriaQuery.from(Derrata.class);
				
		/* predicato del lotto */ 
		Join<Derrata, TipoDerrata> derrataJoin = derrataRoot.join("tipoDerrata");
		
		
		/* predicato finale per la ricerca */
		//where codice = idLotto
		Predicate finalPredicate = criteriaBuilder.equal(derrataJoin.get("codice"), idLotto); 
				
		switch(colonnaEnum)
		{
			case UNITA_MISURA:
				//where codice = id lotto and unita_misura = ??? 
				finalPredicate = criteriaBuilder.and(finalPredicate, criteriaBuilder.like(derrataRoot.get(ColonneDerrataEnum.UNITA_MISURA.getclonnaTipoDerrata()), value + "%"));
				break;
				
			case DESCRIZIONE:
				finalPredicate = criteriaBuilder.like(derrataRoot.get(ColonneDerrataEnum.DESCRIZIONE.getclonnaTipoDerrata()), value + "%");
				break;
			
			case PREZZO:
				//conversione da String a double
				finalPredicate = criteriaBuilder.equal(derrataRoot.get(ColonneDerrataEnum.PREZZO.getclonnaTipoDerrata()), value);
				break;
			
			case GIACENZA:
				//conversione da String a double
				finalPredicate = criteriaBuilder.equal(derrataRoot.get(ColonneDerrataEnum.GIACENZA.getclonnaTipoDerrata()), value);
				break;
			
			case DATA:
				SimpleDateFormat dateFormat = new SimpleDateFormat(this.dateFormat);
				Date data = null;
				try
				{
					data = dateFormat.parse(value);
				}
				catch(Exception e)
				{
					throw new GesevException("Si e' verificato un errore" + ExceptionUtils.getStackFrames(e)[0], HttpStatus.BAD_REQUEST);
				}
				finalPredicate = criteriaBuilder.equal(derrataRoot.get(ColonneDerrataEnum.DATA.getclonnaTipoDerrata()), value);
				break;
			
			case QUANTITA_MINIMA:
				//conversione da String a double o inter controlla poi
				finalPredicate = criteriaBuilder.equal(derrataRoot.get(ColonneDerrataEnum.QUANTITA_MINIMA.getclonnaTipoDerrata()), value);
				break;
			
		}
		
		/* esecuzione query */
		criteriaQuery.where(finalPredicate);
		List<Derrata> items = entityManager.createQuery(criteriaQuery).getResultList();
		
		logger.info("Numero elementi trovati: " + items.size());
		
		/* restituzione */
		return items;
	}

}