/* 上海如静知华信息科技有限公司 https://www.zhuatech.cn/ */
package cn.zhuatech.reconcileai;
import org.springframework.stereotype.Component;
import java.util.*;
import java.time.*;
import static cn.zhuatech.reconcileai.Model.*;
import static cn.zhuatech.reconcileai.Engine.*;

/**
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
@Component public class Domain {
 private final InsightProvider insight;
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public Domain(InsightProvider insight){this.insight=insight;}
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 static String text(Row r,String key){return txt(r.data(),key);}
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public void create(Engine e,User u,String module,Map<String,Object>d){
  if(Set.of("bankEntries","bookEntries").contains(module)){
   require(num(d,"amount").stripTrailingZeros().scale()<=2,"交易金额最多两位小数");
   require(!date(d,"bookedAt").isAfter(LocalDate.now()),"交易日期不能是未来");
   require(e.all(u,module).stream().noneMatch(x->text(x,"sourceId").equalsIgnoreCase(txt(d,"sourceId"))),"来源唯一号重复");
  }
 }
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public void edit(Engine e,User u,Row r,Map<String,Object>d){
  require(r.module().equals("bookEntries"),"银行原始流水不可修改");
  require(num(d,"amount").stripTrailingZeros().scale()<=2&&!date(d,"bookedAt").isAfter(LocalDate.now()),"账务金额或日期无效");
  require(e.all(u,"bookEntries").stream().noneMatch(x->!x.id().equals(r.id())&&text(x,"sourceId").equalsIgnoreCase(txt(d,"sourceId"))),"来源唯一号重复");
  require(e.all(u,"bankEntries").stream().noneMatch(x->x.state().equals("PROPOSED")&&text(x,"candidateBook").equals(r.id())),"已有待复核候选，不得修改账务流水");
 }
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public String action(Engine e,User u,Row r,String action,Map<String,Object>i,Map<String,Object>d){
  switch(r.module()+"."+action){
   case "bankEntries.propose" -> {
    var ranked=insight.candidates(r,e.all(u,"bookEntries"));
    require(!ranked.isEmpty()&&ranked.getFirst().score()>=70,"没有达到匹配阈值的账务流水，请登记差异");
    require(ranked.size()==1||ranked.getFirst().score()>ranked.get(1).score(),"存在同分候选，必须人工核对，不自动选取");
    var candidate=ranked.getFirst();require(e.all(u,"bankEntries").stream().noneMatch(x->x.state().equals("PROPOSED")&&text(x,"candidateBook").equals(candidate.bookId())),"账务流水已有待复核候选");
    e.ledger(u,"proposals","RECORDED",Map.of("bank",r.id(),"book",candidate.bookId(),"score",candidate.score(),"evidence",candidate.evidence(),"method","LOCAL_RULES_V1","proposedBy",u.username()));
    d.put("candidateBook",candidate.bookId());d.put("matchScore",candidate.score());d.put("matchEvidence",candidate.evidence());d.put("proposedAt",Instant.now().toString());
   }
   case "bankEntries.confirm" -> {
    Row book=e.get(u,txt(d,"candidateBook"));require(book.module().equals("bookEntries")&&book.state().equals("OPEN"),"账务流水已被匹配或候选失效");
    var updated=new LinkedHashMap<>(book.data());updated.put("matchedBank",r.id());updated.put("matchedAt",Instant.now().toString());
    e.save(u,book,"MATCHED",updated,"MATCH","独立复核银行流水匹配");
    e.ledger(u,"matches","POSTED",Map.of("bank",r.id(),"book",book.id(),"amount",num(d,"amount"),"score",d.get("matchScore"),"confirmedBy",u.username(),"confirmedAt",Instant.now().toString()));
    d.put("confirmedBy",u.username());d.put("confirmedAt",Instant.now().toString());
   }
   case "bankEntries.reject" -> {d.put("rejectedCandidate",txt(d,"candidateBook"));d.put("rejectionReason",txt(i,"reason"));d.remove("candidateBook");d.remove("matchScore");e.ledger(u,"exceptions","RECORDED",Map.of("bank",r.id(),"type","CANDIDATE_REJECTED","reason",txt(i,"reason"),"reviewedBy",u.username()));}
   case "bankEntries.exception" -> {d.put("exceptionReason",txt(i,"reason"));e.ledger(u,"exceptions","RECORDED",Map.of("bank",r.id(),"type","UNMATCHED","reason",txt(i,"reason"),"recordedBy",u.username()));}
   case "bankEntries.close" -> {d.put("resolution",txt(i,"resolution"));d.put("closedBy",u.username());e.ledger(u,"exceptions","RECORDED",Map.of("bank",r.id(),"type","CLOSED","resolution",txt(i,"resolution"),"closedBy",u.username()));}
  }
  return null;
 }
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public Map<String,Object> metrics(Engine e,User u){var banks=e.all(u,"bankEntries");return Map.of("待复核候选",banks.stream().filter(x->x.state().equals("PROPOSED")).count(),"未匹配流水",banks.stream().filter(x->x.state().equals("UNMATCHED")).count(),"已确认匹配",e.all(u,"matches").size(),"待处理差异",banks.stream().filter(x->x.state().equals("EXCEPTION")).count());}
}
