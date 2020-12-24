

//classe che utilizzo per creare un arraylist di utenti con i loro punti per poi ordinarlo secondo punteggi decrescenti e restituire la classifica
//all'utente che la richiede dato che la classifica non è persistente ogni volta la ricalcolo inserendo elementi di tipo 
//utentiPunti in questo arraylist e riordinandolo con il metodo sort che agisce secondo la definizione del metodo compareTo di questa classe che implementa comparable
public class utentiPunti implements Comparable<utentiPunti>{
	private String nome;
	private int punteggio;
	
	utentiPunti(String nome, int punteggio){
		this.nome=nome;
		this.punteggio=punteggio;
	}
	
	
	//metodi get
	int getPt() {
		return this.punteggio;
	}
	
	String getName() {
		return this.nome;
	}

	//metodo comparatore per ordinare
	public int compareTo(utentiPunti p) {
		if(this.punteggio < p.punteggio) {
			return 1;
		}
		else if(this.punteggio == p.punteggio) {
			return 0;
		}
		else return -1;
	}


}