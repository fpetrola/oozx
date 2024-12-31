package gui;

public class SimilarTimestamp
{
    private long timestamp;

    public SimilarTimestamp(long timestamp)
    {
	this.timestamp= timestamp;
    }

    public boolean equals(Object obj) 
    {
	SimilarTimestamp similarTimestamp= (SimilarTimestamp) obj;

	return Math.abs(similarTimestamp.timestamp - timestamp) < 5;
    }

    public int hashCode()
    {
	return 0;
    }
}
