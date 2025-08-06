package com.aiproject.smartcampus.mapper;


import com.aiproject.smartcampus.pojo.po.TeacherStudentConversation;
import com.aiproject.smartcampus.pojo.po.TeacherStudentConversations;
import com.aiproject.smartcampus.pojo.po.TeacherStudentMessage;
import com.aiproject.smartcampus.pojo.vo.ChatQueryVO;
import com.aiproject.smartcampus.pojo.vo.ConversationUnreadCountVO;
import com.aiproject.smartcampus.pojo.vo.TeacherInfoVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface StudentTeacherChatMapper {

    @Select("SELECT \n" +
            "    conversation_id,\n" +
            "    student_id,\n" +
            "    teacher_id,\n" +
            "    created_at\n" +
            "FROM teacher_student_conversations \n" +
            "WHERE student_id = ? AND teacher_id = ?;")
    ChatQueryVO getChatByTeacherId(@Param(value = "studentId") String studentId, @Param(value = "teacherId") String teacherId);


    @Insert("INSERT INTO teacher_student_conversations (student_id, teacher_id) " +
            "VALUES (#{studentId}, #{teacherId})")
    @Options(useGeneratedKeys = true, keyProperty = "conversationId", keyColumn = "conversation_id")
    int insertConversation(@Param("studentId") String studentId, @Param("teacherId") String teacherId);


    /**
     * 基于学生ID、教师ID和会话ID查询所有消息
     * @param studentId 学生ID
     * @param teacherId 教师ID
     * @param conversationId 会话ID
     * @return 消息列表
     */
    @Select("SELECT m.* FROM teacher_student_messages m " +
            "INNER JOIN teacher_student_conversations c ON m.conversation_id = c.conversation_id " +
            "WHERE c.student_id = #{studentId} AND c.teacher_id = #{teacherId} AND c.conversation_id = #{conversationId} " +
            "ORDER BY m.created_at ASC")
    List<TeacherStudentMessage> getAllMessagesByStudentAndTeacher(@Param("studentId") String studentId,
                                                                  @Param("teacherId") String teacherId,
                                                                  @Param("conversationId") Long conversationId);

    /**
     * 设置指定用户在某个会话中接收的所有消息为已读
     * @param conversationId 会话ID
     * @param receiverUserId 接收者用户ID
     * @return 更新的消息数量
     */
    @Update("UPDATE teacher_student_messages " +
            "SET is_read = TRUE, read_at = NOW() " +
            "WHERE conversation_id = #{conversationId} " +
            "AND sender_user_id != #{receiverUserId} " +
            "AND is_read = FALSE")
    Integer markReceivedMessagesAsRead(@Param("conversationId") String conversationId, @Param("receiverUserId") String receiverUserId);



    /**
     * 查询当前用户所有未读消息数量（学生身份）
     * 统计该学生在所有会话中的未读消息总数
     *
     * @param studentId 学生ID
     * @return 所有未读消息数量
     */
    @Select("SELECT COUNT(*) FROM teacher_student_messages m " +
            "INNER JOIN teacher_student_conversations c ON m.conversation_id = c.conversation_id " +
            "WHERE c.student_id = #{studentId} " +
            "AND m.is_read = FALSE AND m.sender_user_id != #{studentId}")
    Integer getAllUnreadMessageCountForStudent(@Param("studentId") Integer studentId);

    /**
     * 查询当前用户所有未读消息数量（教师身份）
     * 统计该教师在所有会话中的未读消息总数
     *
     * @param teacherId 教师ID
     * @return 所有未读消息数量
     */
    @Select("SELECT COUNT(*) FROM teacher_student_messages m " +
            "INNER JOIN teacher_student_conversations c ON m.conversation_id = c.conversation_id " +
            "WHERE c.teacher_id = #{teacherId} " +
            "AND m.is_read = FALSE AND m.sender_user_id != #{teacherId}")
    Integer getAllUnreadMessageCountForTeacher(@Param("teacherId") String teacherId);


    /**
     * 查询当前用户每个会话的未读消息数量（学生身份）
     *
     * @param studentId 学生ID
     * @return 每个会话的未读消息统计
     */
    @Select("SELECT " +
            "    c.conversation_id, " +
            "    c.teacher_id, " +
            "    t_user.real_name as teacher_name, " +
            "    COUNT(m.message_id) as unread_count " +
            "FROM teacher_student_conversations c " +
            "LEFT JOIN teacher_student_messages m ON c.conversation_id = m.conversation_id " +
            "    AND m.is_read = FALSE AND m.sender_user_id != c.student_id " +
            "INNER JOIN teachers t ON c.teacher_id = t.teacher_id " +
            "INNER JOIN users t_user ON t.user_id = t_user.user_id " +
            "WHERE c.student_id = #{studentId} " +
            "GROUP BY c.conversation_id, c.teacher_id, t_user.real_name " +
            "HAVING COUNT(m.message_id) > 0 " +
            "ORDER BY unread_count DESC")
    List<ConversationUnreadCountVO> getUnreadCountByConversationForStudent(@Param("studentId") String studentId);


    /**
     * 查询当前用户每个会话的未读消息数量（教师身份）
     *
     * @param teacherId 教师ID
     * @return 每个会话的未读消息统计
     */
    @Select("SELECT " +
            "    c.conversation_id, " +
            "    c.student_id, " +
            "    s_user.real_name as student_name, " +
            "    s.student_number, " +
            "    COUNT(m.message_id) as unread_count " +
            "FROM teacher_student_conversations c " +
            "LEFT JOIN teacher_student_messages m ON c.conversation_id = m.conversation_id " +
            "    AND m.is_read = FALSE AND m.sender_user_id != c.teacher_id " +
            "INNER JOIN students s ON c.student_id = s.student_id " +
            "INNER JOIN users s_user ON s.user_id = s_user.user_id " +
            "WHERE c.teacher_id = #{teacherId} " +
            "GROUP BY c.conversation_id, c.student_id, s_user.real_name, s.student_number " +
            "HAVING COUNT(m.message_id) > 0 " +
            "ORDER BY unread_count DESC")
    List<ConversationUnreadCountVO> getUnreadCountByConversationForTeacher(@Param("teacherId") String teacherId);

    @Select("<script>" +
            "SELECT DISTINCT " +
            "    t.teacher_id, " +
            "    u.real_name as teacher_name, " +
            "    c.course_name, " +
            "    t.department, " +
            "    u.status " +
            "FROM teachers t " +
            "INNER JOIN users u ON t.user_id = u.user_id " +
            "INNER JOIN courses c ON t.teacher_id = c.teacher_id " +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id " +
            "WHERE ce.student_id = #{studentId} " +
            "AND u.user_type = 'teacher' " +
            "<if test='courseIds != null and courseIds.size() > 0'>" +
            "AND c.course_id IN " +
            "<foreach collection='courseIds' item='courseId' open='(' separator=',' close=')'>" +
            "#{courseId}" +
            "</foreach>" +
            "</if>" +
            "ORDER BY c.course_name, t.department, u.real_name" +
            "</script>")
    List<TeacherInfoVO> getTeachersWithCourseByStudentIdAndCourseIds(@Param("studentId") String studentId,
                                                                     @Param("courseIds") List<String> courseIds);


    /**
     * 发送消息到数据库
     * @param conversationId 会话ID
     * @param senderUserId 发送者用户ID
     * @param content 消息内容
     * @param messageType 消息类型
     * @param fileUrl 文件URL（可选）
     * @return 影响行数
     */
    @Insert("INSERT INTO teacher_student_messages " +
            "(conversation_id, sender_user_id, content, message_type, file_url, is_read, created_at) " +
            "VALUES (#{conversationId}, #{senderUserId}, #{content}, #{messageType}, #{fileUrl}, FALSE, NOW())")
    int sendMessage(@Param("conversationId") Long conversationId,
                    @Param("senderUserId") Integer senderUserId,
                    @Param("content") String content,
                    @Param("messageType") String messageType,
                    @Param("fileUrl") String fileUrl);


    @Insert("INSERT INTO teacher_student_conversations(student_id, teacher_id)\n" +
            "VALUES(#{studentId},#{teacherId});")
    @Options(
            useGeneratedKeys = true,
            keyProperty = "id",       // 生成的主键值要赋值给实体类的哪个属性（需与实体类属性名一致）
            keyColumn = "conversation_id"          // 数据库表中的主键字段名（需与表结构一致，可省略如果和属性名相同）
    )
    int setConnection(TeacherStudentConversation conversation);

    @Select("SELECT conversation_id FROM teacher_student_conversations WHERE student_id = #{studentId} AND teacher_id = #{teacherId}")
    Long getConnection(TeacherStudentConversation conversation);
}
