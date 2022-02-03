package it.gesev.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="FORNITORE")
@Getter
@Setter
public class Fornitore 
{
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="CODICE")
	private int codice;
	
	@Column(name="DESCRIZIONE")
	private String Descrizione;
	
	@OneToOne(mappedBy="fornitore", cascade={CascadeType.PERSIST, CascadeType.DETACH,
		 	CascadeType.MERGE, CascadeType.REFRESH})
	private TestataMovimento testataMovimento;
	
	public Fornitore()
	{
		
	}

	public Fornitore(int codice, String descrizione, TestataMovimento testataMovimento) {
		this.codice = codice;
		Descrizione = descrizione;
		this.testataMovimento = testataMovimento;
	}
}