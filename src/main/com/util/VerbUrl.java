package main.com.util;

public class VerbUrl {
    private String url; 
    private String methodeHttp; 

    public VerbUrl(String url, String methodeHttp) {
        this.url = url;
        this.methodeHttp = methodeHttp.toUpperCase(); 
    }

    public String getUrl() {
        return url;
    }

    public String getMethodeHttp() {
        return methodeHttp;
    }

    @Override
    public boolean equals(Object obj) {
        
        if (this == obj)
            return true;

        
        if (obj == null || getClass() != obj.getClass())
            return false;

        
        VerbUrl autre = (VerbUrl) obj;
        return this.url.equals(autre.url) &&
                this.methodeHttp.equals(autre.methodeHttp);
    }

    @Override
    public int hashCode() {
        int resultat = url.hashCode();
        resultat = 31 * resultat + methodeHttp.hashCode();
        return resultat;
    }

    @Override
    public String toString() {
        return methodeHttp + " " + url;
    }
}