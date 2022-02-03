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
@Table(name="ENTE")
@Getter
@Setter
public class Ente 
{
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="CODICE_ACED")
	private String codiceACED;
	
	@OneToOne(mappedBy="ente", cascade={CascadeType.PERSIST, CascadeType.DETACH,
		 	CascadeType.MERGE, CascadeType.REFRESH})
	private TestataMovimento testataMovimento;
	
	public Ente()
	{
		
	}

	public Ente(String codiceACED, TestataMovimento testataMovimento) 
	{
		this.codiceACED = codiceACED;
		this.testataMovimento = testataMovimento;
	}
	
	

}