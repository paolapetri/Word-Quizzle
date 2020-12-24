//classe che utilizzo nel thread della sfida del server sfruttando l'elemento parolePunti per inviare le parole da tradurre, salvarmi la traduzione inviata dai due utenti
//e assegnargli il relativo punteggio

public class parolePunti {
	private String parolaITA;
	private String parolaENG;
	private int punti;
	
	public parolePunti(){
		
		//inizializzo a vuoto le stringhe e a 0 i punti
		parolaITA="";
		parolaENG="";
		punti = 0;
	}

	//metodi get e set
	
	public String getParolaITA() {
		return parolaITA;
	}

	public void setParolaITA(String parolaITA) {
		this.parolaITA = parolaITA;
	}

	public String getParolaENG() {
		return parolaENG;
	}

	public void setParolaENG(String parolaENG) {
		this.parolaENG = parolaENG;
	}

	public int getPunti() {
		return punti;
	}

	public void setPunti(int punti) {
		this.punti = punti;
	}
}
