import java.util.LinkedList;
import java.util.Random;

public class Processo {
	
	private int pid;
	private boolean ehCoordenador = false;
	private Thread utilizaRecurso = new Thread();
	private Conexao conexao = new Conexao();
	
	// variaveis utilizadas apenas pelo coordenador
	private LinkedList<Processo> listaDeEspera;
	private boolean recursoEmUso;
	
	private static final int USO_PROCESSO_MIN = 10000;
	private static final int USO_PROCESSO_MAX = 20000;
	
	public Processo(int pid) {
		this.pid = pid;
		setEhCoordenador(false);
	}
	
	public int getPid() {
		return pid;
	}
	
	public boolean isCoordenador() {
		return ehCoordenador;
	}

	public void setEhCoordenador(boolean ehCoordenador) {
		this.ehCoordenador = ehCoordenador;
		if(this.ehCoordenador) {
			listaDeEspera = new LinkedList<>();
			conexao.conectar(this);
			
			if(ControladorDeProcessos.isSendoConsumido())
				ControladorDeProcessos.getConsumidor().interronperAcessoRecurso();
			
			recursoEmUso = false;
		}
	}
	
	private void interronperAcessoRecurso() {
		if(utilizaRecurso.isAlive())
			utilizaRecurso.interrupt();
	}
	
	public boolean isRecursoEmUso() {
		return encontrarCoordenador().recursoEmUso;
	}
	
	public void setRecursoEmUso(boolean estaEmUso, Processo consumidor) {
		Processo coordenador = encontrarCoordenador();
		
		coordenador.recursoEmUso = estaEmUso;
		ControladorDeProcessos.setConsumidor(estaEmUso ? consumidor : null);
	}
	
	private LinkedList<Processo> getListaDeEspera() {
		return encontrarCoordenador().listaDeEspera;
	}
	
	public boolean isListaDeEsperaVazia() {
		return getListaDeEspera().isEmpty();
	}
	
	private void removerDaListaDeEspera(Processo processo) {
		if(getListaDeEspera().contains(processo))
			getListaDeEspera().remove(processo);
	}
	
	private Processo encontrarCoordenador() {
		Processo coordenador = ControladorDeProcessos.getCoordenador();
		
		if(coordenador == null) {
			Eleicao eleicao = new Eleicao();
			coordenador = eleicao.realizarEleicao(this.getPid());
		}
		return coordenador;
	}

	public void acessarRecursoCompartilhado() {
		if(ControladorDeProcessos.isUsandoRecurso(this) || this.isCoordenador())
			return;
		
		String resultado = conexao.realizarRequisicao("Processo " + this + " quer consumir o recurso.\n");
		
		System.out.println("Resultado da requisicao do processo " + this + ": " + resultado);
		
		if(resultado.equals(Conexao.PERMITIR_ACESSO))
			utilizarRecurso(this);
		else if(resultado.equals(Conexao.NEGAR_ACESSO))
			adicionarNaListaDeEspera(this);
	}
	
	private void adicionarNaListaDeEspera(Processo processoEmEspera) {
		getListaDeEspera().add(processoEmEspera);
		
		System.out.println("Processo " + this + " foi adicionado na lista de espera.");
		System.out.println("Lista de espera: " + getListaDeEspera());
	}
	
	private void utilizarRecurso(Processo processo) {
		Random random = new Random();
		int randomUsageTime = USO_PROCESSO_MIN + random.nextInt(USO_PROCESSO_MAX - USO_PROCESSO_MIN);
		
		utilizaRecurso = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Processo " + processo + " está consumindo o recurso.");
				setRecursoEmUso(true, processo);
				
				try {
					Thread.sleep(randomUsageTime);
				} catch (InterruptedException e) { }
				
				System.out.println("Processo " + processo + " parou de consumir o recurso.");
				processo.liberarRecurso();
			}
		});
		utilizaRecurso.start();
	}
	
	private void liberarRecurso() {
		setRecursoEmUso(false, this);
		
		if(!isListaDeEsperaVazia()) {
			Processo processoEmEspera = getListaDeEspera().removeFirst();
			processoEmEspera.acessarRecursoCompartilhado();
			System.out.println("Processo " + processoEmEspera + " foi removido da lista de espera.");
			System.out.println("Lista de espera: " + getListaDeEspera());
		}
	}
	
	public void destruir() {
		if(isCoordenador()) {
			conexao.encerrarConexao();
		} else {
			removerDaListaDeEspera(this);
			if(ControladorDeProcessos.isUsandoRecurso(this)) {
				interronperAcessoRecurso();
				liberarRecurso();
			}
		}
			
		ControladorDeProcessos.removerProcesso(this);
	}

	@Override
	public boolean equals(Object objeto) {
		Processo processo = (Processo) objeto;
		if(processo == null)
			return false;
		
		return this.pid == processo.pid;
	}
	
	@Override
	public String toString() {
		return String.valueOf(this.getPid());
	}
}