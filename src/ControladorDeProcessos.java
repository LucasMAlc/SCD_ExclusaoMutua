import java.util.ArrayList;

public class ControladorDeProcessos {

	private static ArrayList<Processo> processosAtivos = new ArrayList<Processo>();
	private static RecursoCompartilhado recurso = new RecursoCompartilhado();
	private static Processo consumidor = null;
	
	private ControladorDeProcessos() {
		
	}
	
	public static ArrayList<Processo> getProcessosAtivos() {
		return processosAtivos;
	}

	public static ArrayList<Processo> processosAtendidos() {
		return processosAtivos;
	}
	
	public static RecursoCompartilhado getRecurso() {
		return recurso;
	}
	
	public static Processo getConsumidor() {
		return consumidor;
	}

	public static void setConsumidor(Processo novoConsumidor) {
		consumidor = novoConsumidor;
	}
	
	public static Processo getCoordenador() {
		for (Processo processo : processosAtivos) {
			if (processo.isCoordenador())
				return processo;
		}
		return null;
	}
	
	public static void removerProcesso(Processo processo) {
		processosAtivos.remove(processo);
	}

	public static boolean isUsandoRecurso(Processo processo) {
		return processo.equals(consumidor);
	}
	
	public static boolean isSendoConsumido() {
		return consumidor != null;
	}
}