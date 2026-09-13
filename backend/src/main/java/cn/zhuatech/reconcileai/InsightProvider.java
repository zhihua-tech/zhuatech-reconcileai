/* 上海如静知华信息科技有限公司 https://www.zhuatech.cn/ */
package cn.zhuatech.reconcileai;
import org.springframework.stereotype.Component;
import java.util.*;
import java.time.*;
import static cn.zhuatech.reconcileai.Model.*;
import static cn.zhuatech.reconcileai.Engine.*;

/** 银企候选匹配接口；默认规则只提出候选，最终入账需独立复核。 */
public interface InsightProvider {
 record Candidate(String bookId,int score,List<String> evidence){}
 List<Candidate> candidates(Row bank,List<Row> books);
}
@Component class LocalInsightProvider implements InsightProvider {
 public List<Candidate> candidates(Row bank,List<Row> books){
  List<Candidate> results=new ArrayList<>();
  for(Row book:books){
   if(!book.state().equals("OPEN")||!txt(bank.data(),"direction").equals(txt(book.data(),"direction"))||num(bank.data(),"amount").compareTo(num(book.data(),"amount"))!=0)continue;
   long days=Math.abs(java.time.temporal.ChronoUnit.DAYS.between(date(bank.data(),"bookedAt"),date(book.data(),"bookedAt")));
   if(days>5)continue;
   int score=60;List<String> evidence=new ArrayList<>();evidence.add("收付方向及金额一致");
   if(txt(bank.data(),"reference").equalsIgnoreCase(txt(book.data(),"reference"))){score+=30;evidence.add("业务参考号一致");}
   if(txt(bank.data(),"counterparty").equalsIgnoreCase(txt(book.data(),"counterparty"))){score+=10;evidence.add("交易对方一致");}
   if(days<=1){score+=5;evidence.add("交易日期相差不超过一天");}
   results.add(new Candidate(book.id(),Math.min(score,100),List.copyOf(evidence)));
  }
  return results.stream().sorted(Comparator.comparingInt(Candidate::score).reversed().thenComparing(Candidate::bookId)).toList();
 }
}
