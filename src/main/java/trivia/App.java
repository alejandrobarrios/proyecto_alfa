package trivia;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.*;

import java.util.Map;
import java.util.*;
import org.json.JSONObject;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DB;
import org.javalite.activejdbc.LazyList;
import trivia.BasicAuth;
import trivia.User;
import trivia.Question;
import trivia.Option;
import trivia.Statistic;
import trivia.Answered;

import com.google.gson.Gson;

class QuestionParam
{
    String description;
    String category;
    Boolean see;
    ArrayList<OptionParam> options;
}

class OptionParam
{
    String description;
    Boolean correct;
}

public class App
{
    static User currentUser;
    static int identificador;
    static final String RUTA = "/admin";

    public static JSONObject userToJSON(User user){
          JSONObject res = new JSONObject();
          res.put("username",user.get("username"));
          res.put("password",user.get("password"));
          return res;
        }

  public static JSONObject catToJSON(Statistic cat){
        JSONObject res = new JSONObject();
        res.put("category",cat.get("category"));
        res.put("amount_user_right",cat.get("amount_user_right"));
        res.put("amount_user_wrong",cat.get("amount_user_wrong"));
        return res;
      }

  public static JSONObject questionToJSON(Question q){
            JSONObject res = new JSONObject();
            res.put("id",q.get("id"));
            res.put("description",q.get("description"));
            res.put("amount_user_right",q.get("amount_user_right"));
            res.put("amount_user_wrong",q.get("amount_user_wrong"));
            return res;
          }
    public static void main( String[] args ){
        before((request, response) -> {
            if (Base.hasConnection()) {
                Base.close();
            }
            if (!Base.hasConnection())
                Base.open();
            String url = request.pathInfo().substring(0, 6);
            if(!(url.equals(RUTA))){

            System.out.println("//////////////////////////0"+url);

            String headerToken = (String) request.headers("Authorization");
            if (
                headerToken == null ||
                headerToken.isEmpty() ||
                !BasicAuth.authorize(headerToken)
                ) {
                halt(401);
                }
            currentUser = BasicAuth.getUser(headerToken);
          }

        });
        after((request, response) -> {
            Base.close();
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Headers",
                "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
        });

        options("/*", (request, response) -> {
            return "OK";
        });

      get("/allusers3", (req, res) -> {//get all users load
        res.type("application/json");

       LazyList<User> user = User.findAll();
       List<JSONObject> lista = new ArrayList<JSONObject>();
       for(User u: user ){
         JSONObject usr= userToJSON(u);
         lista.add(usr);
       }

       //String json = new Gson().toJson(lista);
       //return json;
       return lista;
     });

     post("/admin/users", (req, res) -> {

         Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
         User user = new User();
         user.set("username", bodyParams.get("username"));
         user.set("password", bodyParams.get("password"));
         user.set("name", bodyParams.get("name"));
         user.set("lastname", bodyParams.get("lastname"));
         user.set("dni", bodyParams.get("dni"));
         user.set("admin", false);

         user.saveIt();

         res.type("application/json");

         return user.toJson(true);
       });

        //guarda en la base de datos los distintos parametros de usuario que se le pasaron
        post("/users", (req, res) -> {

            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
            User user = new User();
            user.set("username", bodyParams.get("username"));
            user.set("password", bodyParams.get("password"));
            user.set("name", bodyParams.get("name"));
            user.set("lastname", bodyParams.get("lastname"));
            user.set("dni", bodyParams.get("dni"));
            user.set("admin", false);
            user.set("point", 0);
            user.set("amount_right", 0);
            user.set("amount_wrong", 0);

            user.saveIt();

            Level level = new Level();
            level.set("user_id", user.get("id"));
            level.set("level_examen_clinica", 0);
            level.set("level_farmacologia", 0);
            level.set("level_enfermedades", 0);
            level.set("level_clinica_medica", 0);
            level.set("level_epidemiologia", 0);
            level.set("level_quirurgica", 0);

            level.saveIt();

            res.type("application/json");

            return user.toJson(true);
        });

        //guarda en la base de datos los distintos parametros de preguntas que se le pasaron
        //asocia una lista de respuestas a una pregunta
        //crea las estadisticas de dicha pregunta, para saber porcentaje de alumos que la respondiron correctamente o no.
        post("/admin/questions", (req, res) -> {
            QuestionParam bodyParams = new Gson().fromJson(req.body(), QuestionParam.class);

            Question question = new Question();
            question.set("description", bodyParams.description);
            question.set("category", bodyParams.category);
            question.set("see", bodyParams.see);
            question.set("amount_user_right", 0);
            question.set("amount_user_wrong", 0);
            question.save();

            Statistic est = Statistic.findFirst("category = ?", bodyParams.category);

            if(est == null){
              Statistic estadisticas = new Statistic();
              estadisticas.set("amount_user_right", 0);
              estadisticas.set("amount_user_wrong", 0);
              estadisticas.set("category", bodyParams.category);
              estadisticas.save();
            }

            for(OptionParam item: bodyParams.options) {
                Option option = new Option();
                option.set("description", item.description).set("correct", item.correct);
                question.add(option);
            }

            res.type("application/json");
            return question.toJson(true);
        });

        //this method search a user in DB by passing a username
        post("/search", (req,res) -> {
            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);

            User u = User.searchUserByUsername((String)bodyParams.get("username"));
            res.type("application/json");

            if(u==null){
                return "Usuario no encontrado";
            }else {
                return u.toJson(true);
            }

        });

        //this method is for give a user the admin privileges.
        post("/admin/convertTo", (req,res) -> {
            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);

            User u = User.searchUserByUsername((String)bodyParams.get("username"));
            res.type("application/json");
            if(u!=null){
                u.set("admin", true);
                u.saveIt();
                return u.toJson(true);
            }
            System.out.println("llegue aca");
            return "no existe";

        });

        post("/admin/statCat", (req,res) -> {
            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);


            Statistic category_stat = Statistic.findFirst("category = ?", bodyParams.get("category"));
            JSONObject st= catToJSON(category_stat);
            List<JSONObject> lista = new ArrayList<JSONObject>();
            lista.add(st);

            res.type("application/json");
            return lista ;

        });

        post("/admin/statQues", (req,res) -> {
            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);


            LazyList<Question> question_stat = Question.where("category = ?", bodyParams.get("category"));
            List<JSONObject> lista = new ArrayList<JSONObject>();
            for(Question ques: question_stat){
              JSONObject st= questionToJSON(ques);
              lista.add(st);
            }
            res.type("application/json");
            return lista ;

        });

        //get all users load
        get("/allusers", (req, res) -> {
            LazyList<User> user = User.findAll();
            List<String> lista = new ArrayList<String>();
            for(User u: user ){
                String usuario = "Su username es: " + u.get("username") + ", su dni es: " + u.get("password");
                lista.add(usuario);
            }
            return lista;
        });

        //get all questions load of a given category
        get("/catquestions", (req, res) -> {
            LazyList<Question> question = Question.findAll();
            List<String> lista = new ArrayList<String>();
            for(Question q: question ){
                String pregunta = "Su categoria es: " + q.get("category") + ", su descripcion es: " + q.get("description");
                lista.add(pregunta);
            }
            return lista;
        });

        post("/getcat", (req, res) -> {

            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);

            LazyList<Question> question = Question.where("category = ?", bodyParams.get("category"));
            Question choice = question.get(0);

            String resp= "{\"category\":"+ choice.toJson(true,"category");
            resp=resp+"}";
            return resp;
        });

        post("/getresp", (req, res) -> {

            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);

            LazyList<Option> answer = Option.where("question_id = ? and description = ?", identificador, bodyParams.get("category"));
            Option choice = answer.get(0);

            String resp= "{\"description\":"+ choice.toJson(true,"description");
            resp=resp+"}";
            return resp;
        });


        //return a question whith his options
        post("/getquestions", (req, res) -> {

            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
            Answered answered = new Answered();
            Question choice = new Question();
            Random aux = new Random();
            Set c = new HashSet();
            int id_q;
            Boolean flag = true;
            User user = currentUser;
            int id_user = (int)user.get("id");
            int itera = 0;
            int cant = 0;
            int a = 0;


            LazyList<Question> question = Question.where("category = ?", bodyParams.get("category"));
            LazyList<Answered> ans = Answered.where("user_id = ? and category = ? ", id_user, bodyParams.get("category"));
            List<Question> listita;

            for(Answered an : ans){
                c.add(an.get("question_id"));
            }

            if(( question.size() > ans.size() ) ) {

                while(flag && (itera <= ans.size())){

                    choice = question.get(cant);
                    identificador = (int)choice.get("id");


                    if(c.contains(identificador)){
                        itera = itera + 1;
                        cant = cant + 1 ;

                    }else{
                        flag = false;

                    }
                }//caso que salga del while por flag = true a choice = question.get(identificador) else corroborar que aun haya preguntas en question

                listita = question.subList((cant),(question.size()));

                if(flag == false){
                    a = aux.nextInt(listita.size());
                    choice = question.get(a);

                }
            }else{
                return "Todo respondido";
            }
            // chequear el caso en que el i sea igual a question.size();return no hay mas


            String resp= "{\"Question\":"+ choice.toJson(true,"description");
            LazyList<Option> option = Option.where("question_id = ?", identificador);
            List<String> lista = new ArrayList<String>();
            int i=1;
            boolean a1 = false;
            boolean b = false;
            boolean c1 = false;
            boolean d = false;
            boolean ciclo = true;
            Integer pos = new Integer(0);

            while(ciclo){
                pos = aux.nextInt(4);

                switch(pos){

                    case 0 : if(a1){

                            }else{
                                Option op1 = option.get(0);
                                resp = resp +", \"Opcion"+i+"\" : "+op1.toJson(true,"description");
                                i = i+1;
                                a1 = true;
                            }
                            break;
                    case 1 : if(b){

                            }else{
                                Option op2 = option.get(1);
                                resp = resp +", \"Opcion"+i+"\" : "+op2.toJson(true,"description");
                                i = i+1;
                                b = true;
                            }
                            break;
                    case 2 : if(c1){

                            }else{
                                Option op3 = option.get(2);
                                resp = resp +", \"Opcion"+i+"\" : "+op3.toJson(true,"description");
                                i = i+1;
                                c1 = true;
                            }
                            break;
                    case 3 : if(d){

                            }else{
                                Option op4 = option.get(3);
                                resp = resp +", \"Opcion"+i+"\" : "+op4.toJson(true,"description");
                                i = i+1;
                                d = true;
                            }
                            break;
                }


                if(a1 && b && c1 && d){
                    ciclo = false;
                }

            }

            resp=resp+"}";
            System.out.println(resp);
            res.type("application/json");
            return resp;
        });

        //obtiene respuesta del usuario y corrobora si es la correcta, luego actualiza las estadisticas
        post("/getanswer", (req, res) -> {

            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
            Statistic stat = new Statistic();
            User user = currentUser;
            Answered answered = new Answered();
            LazyList<Question> quest = Question.where("id = ?", identificador);
            Question choice = quest.get(0);
            String q = choice.getString("category");
            String name_level = new String();

            LazyList<Option> answer = Option.where("question_id = ? and correct = ?", identificador, true);
            Option option_correct = answer.get(0);

            Level level = Level.findFirst("user_id = ?", user.get("id"));

            int point =(int) user.get("point");
            int user_correct =(int) user.get("amount_right");
            int user_incorrect =(int) user.get("amount_wrong");
            int l = 0; //save level status
            int answered_category = 0; //amount answered for categorie
            final int AUX_DIV = 11; //amount for div acording the problem

            stat = Statistic.findFirst("category = ?", choice.get("category"));
            int correct_stat = (int) stat.get("amount_user_right");
            int incorrect_stat = (int) stat.get("amount_user_wrong");

            int correct_question = (int) choice.get("amount_user_right");
            int incorrect_question = (int) choice.get("amount_user_wrong");

            if(option_correct.get("description").equals(bodyParams.get("description"))){
                point = point + 1;
                correct_question = correct_question + 1;
                correct_stat = correct_stat + 1;
                user_correct = user_correct + 1;

                stat.set("amount_user_right", correct_stat);
                choice.set("amount_user_right", correct_question);
                user.set("point", point);
                user.set("amount_right", user_correct);
                user.save();
                stat.save();
                choice.set("see", true);
                choice.save();
                answered.set("question_id",identificador);
                answered.set("user_id",user.get("id"));
                answered.set("category",choice.get("category"));
                answered.save();

                switch (q) {
                    case "examen_clinica":
                        l = level.getInteger("level_examen_clinica");
                        name_level = "level_examen_clinica";
                        break;
                    case "farmacologia":
                        l = level.getInteger("level_farmacologia");
                        name_level = "level_farmacologia";
                        break;
                    case "enfermedades":
                        l = level.getInteger("level_enfermedades");
                        name_level = "level_enfermedades";
                        break;
                    case "clinica_medica":
                        l = level.getInteger("level_medica");
                        name_level = "level_medica";
                        break;
                    case "epidemiologia":
                        l = level.getInteger("level_epidemiologia");
                        name_level = "level_epidemiologia";
                        break;
                    case "quirurgica":
                        l = level.getInteger("level_quirurgica");
                        name_level = "level_quirurgica";
                        break;

                }


                LazyList<Answered> answered_list = Answered.where("user_id = ? and category = ? ", user.getInteger("id"), choice.get("category"));
                Answered answered_level = new Answered();
                boolean flag = true;

                answered_category = answered_list.size();

                if ((answered_category / AUX_DIV) > l){
                    l = (answered_category /AUX_DIV);
                    level.set(name_level, l); //tengo que guardar el valor l en el nivel de la categoria que corresponda
                }
                level.save();

                String resp = "{\"Point"+"\" : "+ user.toJson(true,"point") + ", \"Correctas"+"\" : "+ option_correct.toJson(true,"description");
                resp=resp+"}";
                System.out.println(resp);
                return resp ;
            }
            incorrect_question = incorrect_question + 1;
            incorrect_stat = incorrect_stat + 1;
            user_incorrect = user_incorrect + 1;

            stat.set("amount_user_wrong", incorrect_stat);
            choice.set("amount_user_wrong", incorrect_question);
            user.set("amount_wrong", user_incorrect);
            user.save();
            stat.save();
            choice.save();
            String resp = "{\"Point"+"\" : "+ user.toJson(true,"point") + ", \"Correctas"+"\" : "+ option_correct.toJson(true,"description");
            resp=resp+"}";
            res.type("application/json");
            return resp ;
        });

        //por ahora no se utiliza
        get("/users", (req, res) -> {//verfication that a user is load
            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
            LazyList<User> users = User.where("username = ?", bodyParams.get("username"));

            if(users.size() > 0){
                User user = users.get(0);
                String pass = (String)bodyParams.get("password");
                if(user.get("password").equals(pass)){
                    currentUser = user;
                    return "Usuario logueado";
                }else{
                    return "Password incorrecta.";
                }
            }
            return "Username incorrecto";
        });

        //delete an user
        delete("/users", (req, res) -> {

            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);

            User user = User.findFirst("username = ?", bodyParams.get("username"));
            user.delete();
            return "borrado";
        });

        //delete an question
        delete("/admin/delquestions", (req, res) -> {

            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
            Question question = Question.findFirst("id = ?", bodyParams.get("id"));
            question.delete();
            return "borrada";
        });

        //modify a password of a user with his username
        put("/users", (req,res) -> {

            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
            User user = User.findFirst("username = ?", bodyParams.get("username"));
            user.set("password", bodyParams.get("password"));
            user.saveIt();

            res.type("application/json");
            return user.toJson(true);
        });

        //modify the description of a question with his id
        put("/questions", (req,res) -> {

            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
            Question question = Question.findFirst("id = ?", bodyParams.get("id"));
            question.set("description", bodyParams.get("description"));
            question.saveIt();

            res.type("application/json");
            return question.toJson(true);
        });

        //envia el puntaje de un usuario
        post("/stats", (req, res) -> {
            User user = currentUser;
            res.type("application/json");
            Level levels = Level.findFirst("user_id = ?", currentUser.get("id"));

            String stat= "{\"Point"+"\" : "+user.toJson(true,"point") +
            ", \"Level_examen_clinica\" : "+ levels.toJson(true,"level_examen_clinica") +
            ", \"Level_farmacologia\" : "+ levels.toJson(true,"level_farmacologia") +
            ", \"Level_enfermedades\" : "+levels.toJson(true,"level_enfermedades") +
            ", \"Level_clinica_medica\" : "+levels.toJson(true,"level_clinica_medica") +
            ", \"Level_epidemiologia\" : "+levels.toJson(true,"level_epidemiologia") +
            ", \"Level_quirurgica\" : "+levels.toJson(true,"level_quirurgica");

             stat=stat+"}";
             return stat;

        });

        //envia las estadisticas de un usuario
        post("/allstats", (req, res) -> {

            User user = currentUser;
            res.type("application/json");

            String stat= "{\"Point"+"\" : "+user.toJson(true,"point") + ", \"Correctas\" : "+user.toJson(true,"amount_right")+ ", \"Incorrectas\" : "+user.toJson(true,"amount_wrong");
            stat=stat+"}";
            return stat;
        });

        //envia los diez usuarios con los mejores puntajes
        post("/allscore", (req, res) -> {

            LazyList<User> users = User.findAll().limit(10).orderBy("point desc");
            int i = 2;
            User user = users.get(0);
            String resp= "{\"Point1\":"+ user.toJson(true,"point") + ", \"User1\" : "+ user.toJson(true,"username") ;
            users.remove(0);
            for(User o : users){
                resp = resp +", \"Point"+i+"\" : " + o.toJson(true,"point") + ", \"User"+i+"\" : "+o.toJson(true,"username");
                i++;
            };
            resp=resp+"}";
            res.type("application/json");
            return resp;
        });

        post("/login", (req, res) -> {
            res.type("application/json");
            // if there is currentUser is because headers are correct, so we only
            // return the current user here
            return currentUser.toJson(true);
        });

        post("/admin/login", (req, res) -> {
            Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
            res.type("application/json");
            User u = User.searchUserByUsername((String)bodyParams.get("username"));
            res.type("application/json");
            if(u != null){

              if((Boolean)u.get("admin")){

                User adminCurrentUser = new User();

                adminCurrentUser.set("username", bodyParams.get("username"));
                adminCurrentUser.set("password", bodyParams.get("password"));

                System.out.println(adminCurrentUser);

                return adminCurrentUser.toJson(true);

              }else {
                System.out.println("aca");
                  return "usted no tiene permiso";
              }
            }
            System.out.println("aca abajo");
            return "No se encuentra el usuario";
        });
    }
}
