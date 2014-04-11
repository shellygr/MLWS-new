package OrOmerShelly;

public class ImpressionParamtersDistributionKey {

	private ImpressionParameters impParams;
	private Double	weight;
		
	public ImpressionParamtersDistributionKey(ImpressionParameters impParams,
			Double weight) {
		super();
		this.impParams = impParams;
		this.weight = weight;
	}

	public ImpressionParameters getImpParams() {
		return impParams;
	}
	
	public void setImpParams(ImpressionParameters impParams) {
		this.impParams = impParams;
	}
	
	public Double getWeight() {
		return weight;
	}
	
	public void setWeight(Double weight) {
		this.weight = weight;
	}
	
	public String toString() {
		return "ImpParams = " + impParams + " ; Weight = " + weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((impParams == null) ? 0 : impParams.hashCode());
		result = prime * result + ((weight == null) ? 0 : weight.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImpressionParamtersDistributionKey other = (ImpressionParamtersDistributionKey) obj;
		if (impParams == null) {
			if (other.impParams != null)
				return false;
		} else if (!impParams.equals(other.impParams))
			return false;
		if (weight == null) {
			if (other.weight != null)
				return false;
		} else if (!weight.equals(other.weight))
			return false;
		return true;
	}
	
	
	
}
