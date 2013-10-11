package meshonandroid.logging;

public class RequestInfo {

    private long id;
    private long start;
    private long end;
    private int csize;
    private String res;
    private String host;

    public RequestInfo(){
    }

    public long getId() {
        return id;
      }
      public void setId(long id) {
        this.id = id;
      }
      public void setStartTime(long v){
          start = v;
      }
      public long getStartTime(){
          return start;
      }
      public void setEndTime(long e){
          end = e;
      }
      public long getEndTime(){
          return end;
      }
      public void setContSize(int s){
          csize = s;
      }
      public int getContSize(){
          return csize;
      }
      public String getRes(){
          return res;
      }
      public void setRes(String r){
          res = r;
      }
      public String getHost(){
          return host;
      }
      public void setHost(String h){
          host = h;
      }
}
