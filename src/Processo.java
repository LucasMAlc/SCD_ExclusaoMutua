import java.util.LinkedList;
import java.util.Random;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;

public class Processo {
	
	String nomeArquivo = "Processo.txt";
	String logCoordenadorTxt = "logCoordenador.txt";
	private int pid;
	private boolean ehCoordenador = false;
	private Thread utilizaRecurso = new Thread();
	private Conexao conexao = new Conexao();
	
	// variaveis utilizadas apenas pelo coordenador
	private LinkedList<Processo> listaDeEspera;
	private boolean recursoEmUso;
	
	private static final int USO_PROCESSO_MIN = 10000;
	private static final int USO_PROCESSO_MAX = 20000;

	private static ArrayList<Integer> totalProcessos = new ArrayList<Integer>();
	private static ArrayList<Integer> qtdProcessosExecutados = new ArrayList<Integer>();
	
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
		
		try {
			String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
			FileWriter myWriter = new FileWriter(nomeArquivo, true);
			myWriter.write("Processo " + consumidor + " esta consumindo o recurso em: " + timeStamp + "\n");
			myWriter.close();

			int aux = totalProcessos.indexOf(consumidor.getPid());

			int qtd = qtdProcessosExecutados.get(aux) + 1;
  
			qtdProcessosExecutados.set(aux, qtd);
		  } catch (IOException e) {
 			e.printStackTrace();
		  }
	}

	public static ArrayList<Integer> getProcessosAtendidos() {
		return totalProcessos;
	}

	public static ArrayList<Integer> getQtdAtendimentos() {
		return qtdProcessosExecutados;
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
		if(ControladorDeProcessos.isUsandoRecurso(this) || this.isCoordenador()){
			return;
		}
		try {
			String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
			FileWriter logCoordenador = new FileWriter(logCoordenadorTxt, true);
			ArrayList<String> mensagem2 = new ArrayList<String>();
			mensagem2.add("1");
			mensagem2.add(Integer.toString(this.getPid()));
			logCoordenador.write("Mensagem: " + mensagem2 + " Horario: " + timeStamp + "\n");
			logCoordenador.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		String resultado = conexao.realizarRequisicao("Processo " + this + " quer consumir o recurso.\n");
	
		if(resultado.equals(Conexao.PERMITIR_ACESSO)){
			try {
				String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
						.format(Calendar.getInstance().getTime());
				FileWriter logCoordenador = new FileWriter(logCoordenadorTxt, true);
				ArrayList<String> mensagem = new ArrayList<String>();
				mensagem.add("2");
				mensagem.add(Integer.toString(this.getPid()));
				logCoordenador.write("Mensagem: " + mensagem + " Horario: " + timeStamp + "\n");
				logCoordenador.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			utilizarRecurso(this);
		}

		else if(resultado.equals(Conexao.NEGAR_ACESSO))
			adicionarNaListaDeEspera(this);
	}
	
	private void adicionarNaListaDeEspera(Processo processoEmEspera) {
		getListaDeEspera().add(processoEmEspera);
		
	}
	
	private void utilizarRecurso(Processo processo) {
		Random random = new Random();
		int randomUsageTime = USO_PROCESSO_MIN + random.nextInt(USO_PROCESSO_MAX - USO_PROCESSO_MIN);
		
		utilizaRecurso = new Thread(new Runnable() {
			@Override
			public void run() {
				setRecursoEmUso(true, processo);
				
				try {
					Thread.sleep(randomUsageTime);
				} catch (InterruptedException e) { }
				
				try {
					String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
					FileWriter logCoordenador = new FileWriter(logCoordenadorTxt, true);
					ArrayList<String> mensagem = new ArrayList<String>();
					mensagem.add("3");
					mensagem.add(Integer.toString(processo.getPid()));
					logCoordenador.write("Mensagem: " + mensagem + " Horario: " + timeStamp + "\n");
					logCoordenador.close();
				  } catch (IOException e) {
					  e.printStackTrace();
				  }
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