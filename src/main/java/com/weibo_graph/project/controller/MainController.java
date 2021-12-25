package com.weibo_graph.project.controller;


import com.sun.org.glassfish.gmbal.ParameterNames;
import javafx.beans.binding.ObjectExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@CrossOrigin(origins = "http://localhost:8080", maxAge = 3600)
@RestController
public class MainController {
    @Resource
    public JdbcTemplate jdbcTemplate;
    @GetMapping(value = "test")
    public List get_every_thing() {
        return jdbcTemplate.queryForList("select * from user_item");
    }

    @GetMapping(value="sweibo_category")
    public List get_weibo_category(){
        List<Map<String, Object>> weibo_category = jdbcTemplate.queryForList("select distinct type from weibo_item where type is not null");
        System.out.println(weibo_category);
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, Object> category: weibo_category){
            Map<String, String> item = new HashMap<>();
            System.out.println(category);
            String type_name = category.get("type").toString();
            item.put("value",type_name);
            item.put("label", type_name);
            result.add(item);
        }
        return result;
    }
    @GetMapping(value="get_proportion")
    public List get_proportion(){
        List<Map<String, Object>> proportion_date = jdbcTemplate.queryForList("select * from proportion_date order by date asc");
        List dates = new ArrayList();
        List proportions = new ArrayList();
        for(Map part: proportion_date){
            String date = part.get("date").toString();
            dates.add(date);
            String proportion = part.get("proportion").toString();
            proportions.add(proportion);
        }
        List pd = new ArrayList();
        pd.add(dates);
        pd.add(proportions);
        System.out.println(pd);
        return pd;
    }
    @GetMapping(value="get_category_proportion")
    public List get_category_proportion(){
        List<Map<String, Object>> category_proportion =
                jdbcTemplate.queryForList("select type, count(weibo_id) from weibo_item where type is not null and date(timestamp)>'2019-12-30' group by type");
        List result = new ArrayList();
        List labels = new ArrayList();
        List labels_value = new ArrayList();
        for(Map part: category_proportion){
            Map<String, String> item = new HashMap<>();
            String category = part.get("type").toString();
            String value = part.get("count").toString();
            labels.add(category);
            item.put("value",value);
            item.put("name", category);
            labels_value.add(item);
        }
        result.add(labels);
        result.add(labels_value);
        System.out.println(result);
        return result;
    }
    @GetMapping(value="get_word_counts")
    public List get_word_counts(@RequestParam String type){
        System.out.println(type);
        List word_counts = jdbcTemplate.
                queryForList("select key_word as name, count(comments_id) " +
                        "as value from key_word_of_comment where type = '"+type+"'group by key_word having count(comments_id) > 20\n");
        return word_counts;
    }

    @GetMapping(value="get_text_score")
    public List get_text_score(){
        List<Map<String, Object>> query_result = jdbcTemplate.queryForList("select round(cast(score as numeric),2),count(comments_id) " +
                "from text_score group by round(cast(score as numeric),2) order by count(comments_id) desc\n");
        List result = new ArrayList();
        for(Map part_result:query_result){
            List temp = new ArrayList();
            double x = Double.valueOf(part_result.get("round").toString());
            double y = Double.valueOf(part_result.get("count").toString());
            temp.add(x);
            temp.add(y);
            result.add(temp);
        }
        System.out.println(result);
        return result;
    }
    @GetMapping(value="get_comment_text")
    public List get_text_comment(@RequestParam String type, @RequestParam String keyword) {
        List<Map<String, Object>> query_result = jdbcTemplate.queryForList("select comments_id,user_name, text, created_at," +
                "like_counts from comment where comments_id in\n" +
                "                            (select distinct comments_id from key_word_of_comment where key_word = '"+keyword+"'" +
                " and type = '"+type+"')\n" +
                "                            order by like_counts desc limit 10");
        for(Map item: query_result)
            System.out.println(item);
        return query_result;
    }
    @GetMapping(value="get_similar_text")
    public List get_similar_text(@RequestParam String comments_id){
        System.out.println(comments_id);
        List result = new ArrayList();
        List<Map<String, Object>> query_result =
                jdbcTemplate.queryForList("select comments_id, user_name, text, created_at, like_counts from comment where" +
                        " comments_id in (select comments_id_2 from text_similarity " +
                        "where (comments_id_1 = '"+comments_id+"' or comments_id_2= '"+comments_id+"') order by similarity) ");
        //返回一个text_score
        List<Map<String, Object>> query_for_text_score =
                jdbcTemplate.queryForList("select concat('评论得分：',round(cast(score as numeric),1)) as name, " +
                        "count(round(cast(score as numeric),1)) as value from text_score " +
                        "where comments_id in (select comments_id from comment where comments_id in " +
                        "(select comments_id_2 from text_similarity where comments_id_1 = '"+comments_id+"'" +
                        " order by similarity)) group by round(cast(score as numeric),1)");
        result.add(query_result);
        result.add(query_for_text_score);
        return result;
    }
    @GetMapping(value="get_policy_counts")
    public List get_policy_counts(){
        List query_result = jdbcTemplate.queryForList("select location as name, count(distinct weibo_id) as value from local_policy group by location");
        return query_result;
    }
    @GetMapping(value="get_local_policy")
    public List get_local_policy(@RequestParam String province){
        System.out.println(province);
        List query_result = jdbcTemplate.queryForList("select a.weibo_id, a.content as content,a.timestamp " +
                "as timestamp from weibo_item a, local_policy" +
                " where a.weibo_id = local_policy.weibo_id " +
                "and local_policy.location='"+province+"' order by date(a.timestamp)");
        return query_result;
    }
    @GetMapping(value="get_education_info")
    public List get_education_info(@RequestParam String grade){
        List query_result = jdbcTemplate.queryForList("select * from weibo_item where type like '%教育%'" +
                " and content like '%"+grade+"%'");
        return query_result;
    }
    @GetMapping(value="get_gk_attitudes")
    public List get_gk_attitudes(){
        List support = jdbcTemplate.queryForList("select * from comment where comment.weibo_id in (select distinct" +
                " weibo_item.weibo_id from weibo_item where content like '%高考%' ) and (comment.text like '%推迟%' and " +
                "(comment.text not like '%别%' and comment.text not like '%不%'))");
        List oppose = jdbcTemplate.queryForList("select * from comment where comment.weibo_id in (select distinct" +
                " weibo_item.weibo_id from weibo_item where content like '%高考%' ) and (comment.text like '%推迟%' and" +
                " (comment.text like '%别%' or comment.text like '%不%'))");
        List gaokao_attitudes = jdbcTemplate.queryForList("Select * From gaokao_attitudes Where id In (Select Min(id)" +
                " From gaokao_attitudes Group By category) and category in (select category from gaokao_attitudes " +
                "group by category having count(category) > 10) ");
        List<Map<String, String>> counts = new ArrayList<>();
        double support_len = support.size();
        double oppose_len = oppose.size();
        Map<String, String> temp = new HashMap<>();
        temp.put("name","支持不推迟高考");
        temp.put("value",String.valueOf((support_len/(support_len+oppose_len))*100));
        counts.add(temp);
        Map<String, String> temp2 = new HashMap<>();
        temp2.put("name","支持推迟高考");
        temp2.put("value", String.valueOf((oppose_len/(support_len+oppose_len))*100));
        counts.add(temp2);
        List result = new ArrayList();
        result.add(support);
        result.add(oppose);
        result.add(counts);
        result.add(gaokao_attitudes);
        return result;
    }
    @GetMapping(value="get_rumors")
    public List get_rumors(){
        List query_result = jdbcTemplate.queryForList("select * from rumors");
        return query_result;
    }
    @GetMapping(value="get_people_to_topic")
    public List get_people_to_rumors(@RequestParam String weibo_id, @RequestParam String type){
        System.out.println(type);
        List tabledata = jdbcTemplate.queryForList("select * from " +
                                                "comment where weibo_id = '"+weibo_id+"'");
        List<Map<String, Object>> pie_data = jdbcTemplate.queryForList("select round(cast(score as numeric),1) as name,count(comments_id) as value" +
                " from text_score where comments_id in (select comments_id from comment where weibo_id" +
                " = '"+weibo_id+"') group by round(cast(score as numeric),1) order by count(comments_id) desc");
        Map<String, Integer> scale_counts = new HashMap<String, Integer>();
        scale_counts.put("负面评论",0);
        scale_counts.put("正面评论",0);
        scale_counts.put("中性评论",0);
        int low_counts = 0, high_counts=0, normal_counts = 0;
        for(Map<String, Object> item: pie_data){
            double score = Double.valueOf(item.get("name").toString());
            if(score<0.3){
                low_counts += Integer.valueOf(item.get("value").toString());
                scale_counts.put("负面评论",low_counts);
            }
            else if(score<0.8&&score>0.3){
                normal_counts += Integer.valueOf(item.get("value").toString());
                scale_counts.put("中性评论",normal_counts);
            }
            else{
                high_counts += Integer.valueOf(item.get("value").toString());
                scale_counts.put("正面评论",high_counts);
            }
        }
        Map one = new HashMap();
        one.put("name","负面评论");
        one.put("value", scale_counts.get("负面评论"));
        Map two = new HashMap();
        two.put("name","中性评论");
        two.put("value",scale_counts.get("中性评论"));
        Map three = new HashMap();
        three.put("name","正面评论");
        three.put("value",scale_counts.get("正面评论"));
        List map_list = new ArrayList();
        map_list.add(one);
        map_list.add(two);
        map_list.add(three);
        if(type.equals("谣言")){
            List wordcloud = jdbcTemplate.queryForList("select count(comments_id) as value, key_word as name from " +
                    "rumor_comment_key_word where comments_id in (select distinct comments_id from comment where" +
                    " weibo_id = '"+weibo_id+"') group by key_word having count(comments_id) > 2");

            List result = new ArrayList();
            result.add(tabledata);
            result.add(wordcloud);
            result.add(map_list);
            System.out.println(tabledata);
            return result;
        }
        else if(type.equals("热门微博")){
            List more_data = jdbcTemplate.queryForList("select * from comment where weibo_id = '"+weibo_id+"' " +
                    "or weibo_id in (select weibo_id from weibo_item where tag = (select distinct tag from weibo_item " +
                    "where weibo_id = '"+weibo_id+"'))");
            List word_cloud = jdbcTemplate.queryForList("select count(comments_id) as value, key_word as name from" +
                    " summary_key_word where comments_id in (select distinct comments_id from comment where " +
                    "weibo_id = '"+weibo_id+"' or weibo_id in (select weibo_id from weibo_item where tag = " +
                    "(select distinct tag from weibo_item where weibo_id = '"+weibo_id+"'))) group by key_word having " +
                    "count(comments_id) > 2");
            System.out.println("select count(comments_id) as value, key_word as name from" +
                    " summary_key_word where comments_id in (select distinct comments_id from comment where " +
                    "weibo_id = '"+weibo_id+"' or weibo_id in (select weibo_id from weibo_item where tag = " +
                    "(select distinct tag from weibo_item where weibo_id = '"+weibo_id+"'))) group by key_word having " +
                    "count(comments_id) > 2");
            List result = new ArrayList();
            result.add(more_data);
            result.add(word_cloud);
            result.add(map_list);
            return result;
        }
        else if(type.equals("教育")){
            List wordcloud = jdbcTemplate.queryForList("select count(comments_id) as value, key_word as name from " +
                    "education_comment_key_word where comments_id in (select distinct comments_id from comment where" +
                    " weibo_id = '"+weibo_id+"') group by key_word having count(comments_id) > 2");
            List result = new ArrayList();
            result.add(tabledata);
            result.add(wordcloud);
            result.add(map_list);
            System.out.println(tabledata);
            return result;
        }
        else if(type.equals("政策")) {
            List wordcloud = jdbcTemplate.queryForList("select count(comments_id) as value, key_word as name from " +
                    "policy_comment_key_word where comments_id in (select distinct comments_id from comment where" +
                    " weibo_id = '" + weibo_id + "') group by key_word");
            List result = new ArrayList();
            result.add(tabledata);
            result.add(wordcloud);
            result.add(map_list);
            return result;
        }
        else if(type.equals("复工")){
            List wordcloud = jdbcTemplate.queryForList("select count(comments_id) as value, key_word as name from " +
                    "work_comment_key_word where comments_id in (select distinct comments_id from comment where" +
                    " weibo_id = '" + weibo_id + "') group by key_word");
            List result = new ArrayList();
            result.add(tabledata);
            result.add(wordcloud);
            result.add(map_list);
            return result;
        }
        else if(type.equals("交通")){
            List wordcloud = jdbcTemplate.queryForList("select count(comments_id) as value, key_word as name from " +
                    "transportation_comment_key_word where comments_id in (select distinct comments_id from comment where" +
                    " weibo_id = '" + weibo_id + "') group by key_word");
            List result = new ArrayList();
            result.add(tabledata);
            result.add(wordcloud);
            result.add(map_list);
            System.out.println(wordcloud);
            return result;
        }
        else if(type.equals("海外疫情")){
            List wordcloud = jdbcTemplate.queryForList("select count(comments_id) as value, key_word as name from " +
                    "oversea_epidemic_comment_key_word where comments_id in (select distinct comments_id from comment where" +
                    " weibo_id = '" + weibo_id + "') group by key_word");
            List result = new ArrayList();
            result.add(tabledata);
            result.add(wordcloud);
            result.add(map_list);
            System.out.println(tabledata);
            return result;
        }
        else if(type.equals("境外输入")){
            List wordcloud = jdbcTemplate.queryForList("select count(comments_id) as value, key_word as name from " +
                    "oversea_input_comment_key_word where comments_id in (select distinct comments_id from comment where" +
                    " weibo_id = '" + weibo_id + "') group by key_word");
            List result = new ArrayList();
            result.add(tabledata);
            result.add(wordcloud);
            result.add(map_list);
            System.out.println(tabledata);
            return result;
        }
//        else if(type.equals("红十字会")){
//            List wordcloud = jdbcTemplate.queryForList("select count(comments_id) as value, key_word as name from " +
//                    "red_cross_comment_key_word where comments_id in (select distinct comments_id from comment where" +
//                    " weibo_id = '" + weibo_id + "') group by key_word");
//            List result = new ArrayList();
//            result.add(tabledata);
//            result.add(wordcloud);
//            System.out.println(tabledata);
//            return result;
//        }
        else{
            return null;
        }
    }
    @GetMapping(value = "get_key_word_comment")
    public List get_key_word_comment(@RequestParam String weibo_id, @RequestParam String key_word, @RequestParam String type){
        if(type.equals("谣言")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select" +
                    " comments_id from rumor_comment_key_word where key_word='"+key_word+"') and weibo_id = '"+weibo_id+"'");
            return query_result;
        }
        else if(type.equals("教育")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select" +
                    " comments_id from education_comment_key_word where key_word='"+key_word+"') and weibo_id = '"+weibo_id+"'");
            return query_result;
        }
        else if(type.equals("政策")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select" +
                    " comments_id from policy_comment_key_word where key_word='"+key_word+"') and weibo_id = '"+weibo_id+"'");
            return query_result;
        }
        else if(type.equals("复工")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select" +
                    " comments_id from work_comment_key_word where key_word='"+key_word+"') and weibo_id = '"+weibo_id+"'");
            return query_result;
        }
        else if(type.equals("交通")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select" +
                    " comments_id from transportation_comment_key_word where key_word='"+key_word+"') and weibo_id = '"+weibo_id+"'");
            return query_result;
        }
        else if(type.equals("海外疫情")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select" +
                    " comments_id from oversea_epidemic_comment_key_word where key_word='"+key_word+"') and weibo_id = '"+weibo_id+"'");
            return query_result;
        }
        else if(type.equals("境外输入")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select" +
                    " comments_id from oversea_input_comment_key_word where key_word='"+key_word+"') and weibo_id = '"+weibo_id+"'");
            return query_result;
        }
        else{
            return null;
        }
    }
    @GetMapping(value="get_school_day")
    public List get_school_day(@RequestParam String grade){
        List school_open = jdbcTemplate.queryForList("select * from weibo_item where type like '%教育%' and " +
                "content like '%"+grade+"%' and (content like '%明确开学%' or content like '%正式开学%');");
        List school_not_open = jdbcTemplate.queryForList("select * from weibo_item where type like '%教育%' and" +
                " content like '%"+grade+"%' and (content not like '%明确开学%' and content not like '%正式开学%');");
        List result = new ArrayList();
        result.add(school_open);
        result.add(school_not_open);
        return result;
    }
    @GetMapping(value = "get_work_condition")
    public List get_work_condition() {
        List work_condition = jdbcTemplate.queryForList("select * from weibo_item where type like '%复工%' order by date(timestamp)");
        return work_condition;
    }
    @GetMapping(value="get_transportation")
    public List get_transportation(){
        List transportation = jdbcTemplate.queryForList("select * from weibo_item where type like '%交通%' and " +
                "date(timestamp) > '2020-01-20' order by date(timestamp)");
        return transportation;
    }
    @GetMapping(value = "get_oversea_input")
    public List get_oversea_input(){
        List oversea_input = jdbcTemplate.queryForList("select * from weibo_item where content like '%境外输入%'");
        return oversea_input;
    }
    @GetMapping(value = "get_oversea_epidemic")
    public List get_oversea_epidemic(@RequestParam String region){
        List oversea_epidemic = jdbcTemplate.queryForList("select * from weibo_item where content " +
                "like '%"+region+"%' and content like '%确诊%' order by date(timestamp)");
        return oversea_epidemic;
    }
    @GetMapping(value = "red_cross_opinion")
    public List red_cross_opinion(){
        List red_cross_opinion = jdbcTemplate.queryForList("select key_word as name, counts as value from red_cross_key_counts");
        List<Map<String, Object>> red_cross_text_score = jdbcTemplate.queryForList("select round(cast(score as numeric),1) as name," +
                "count(comments_id) as value from red_cross_text_score group by round(cast(score as numeric),1)" +
                " order by count(comments_id) desc");
        Map<String, Integer> scale_counts = new HashMap<String, Integer>();
        scale_counts.put("负面评论",0);
        scale_counts.put("正面评论",0);
        scale_counts.put("中性评论",0);
        int low_counts = 0, high_counts=0, normal_counts = 0;
        for(Map<String, Object> item: red_cross_text_score){
            double score = Double.valueOf(item.get("name").toString());
            if(score<0.3){
                low_counts += Integer.valueOf(item.get("value").toString());
                scale_counts.put("负面评论",low_counts);
            }
            else if(score<0.8&&score>0.3){
                normal_counts += Integer.valueOf(item.get("value").toString());
                scale_counts.put("中性评论",normal_counts);
            }
            else{
                high_counts += Integer.valueOf(item.get("value").toString());
                scale_counts.put("正面评论",high_counts);
            }
        }
        Map one = new HashMap();
        one.put("name","负面评论");
        one.put("value", scale_counts.get("负面评论"));
        Map two = new HashMap();
        two.put("name","中性评论");
        two.put("value",scale_counts.get("中性评论"));
        Map three = new HashMap();
        three.put("name","正面评论");
        three.put("value",scale_counts.get("正面评论"));
        List map_list = new ArrayList();
        map_list.add(one);
        map_list.add(two);
        map_list.add(three);
        List result = new ArrayList();
        result.add(red_cross_opinion);
        result.add(red_cross_text_score);
        result.add(map_list);
        System.out.println(red_cross_text_score);
        return result;
    }
    @GetMapping(value="get_hot_weibo")
    public List get_hot_weibo(){
        List query_result = jdbcTemplate.queryForList("select * from unique_weibo_table where date(timestamp)" +
                " >'2020-01-01' and type is not null order by repost_count desc limit 50");
        return query_result;
    }

    @GetMapping(value= "score_comment")
    public List red_cross_score_comment(@RequestParam String scale, @RequestParam String type){
        System.out.println(type);
        if(scale.equals("负面评论")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in" +
                    " (select comments_id from text_score where cast(score as numeric )<0.3 and type like '%"+type+"%') order by like_counts desc limit 100");
            return query_result;
        }
        else if(scale.equals("中性评论")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in" +
                    " (select comments_id from text_score where (cast(score as numeric )<0.8 " +
                    "and cast(score as numeric )>0.3)  and type like '%"+type+"%')");
            return query_result;
        }
        else if(scale.equals("正面评论")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in" +
                    " (select comments_id from text_score where (cast(score as numeric )<1 and " +
                    "cast(score as numeric )>0.8) and type like '%"+type+"%')");
            return query_result;
        }
        else{
            return null;
        }
    }
    @GetMapping(value = "/get_score_scale")
    public List comment_score_scale(@RequestParam String scale, @RequestParam String weibo_id){
        System.out.println(scale);
        if(scale.equals("负面评论")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select " +
                    "comments_id from text_score where (cast(score as numeric)<0.3) and comments_id in (select comments_id from comment where weibo_id = '"+weibo_id+"'))");
            return query_result;
        }
        else if(scale.equals("中性评论")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select " +
                    "comments_id from text_score where (cast(score as numeric)>0.3 and cast(score as numeric)<0.8) and comments_id in (select comments_id from comment where weibo_id = '"+weibo_id+"'))");
            return query_result;
        }
        else if(scale.equals("正面评论")){
            List query_result = jdbcTemplate.queryForList("select * from comment where comments_id in (select " +
                    "comments_id from text_score where (cast(score as numeric)>0.8 and cast(score as numeric)<1) and comments_id in (select comments_id from comment where weibo_id = '"+weibo_id+"'))");
            return query_result;
        }
        else{
            return null;
        }
    }
    @GetMapping(value="reset")
    public List reset(@RequestParam String weibo_id){
        List query_result = jdbcTemplate.queryForList("select * from comment where weibo_id = '"+weibo_id+"'");
        return query_result;
    }


}
