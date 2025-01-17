package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import com.razorpay.RazorpayException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.razorpay.*;
import com.smart.dao.ContcatRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	@Autowired
	private UserRepository userRepository;
	

	@Autowired
	private ContcatRepository contactRepository;
	
	@Autowired
	private MyOrderRepository myOrderRepository;
	
	@ModelAttribute
	public void addCommonData(Model m,Principal principal)
	{
		String userName=principal.getName();
		System.out.println("USERNAME"+userName);
		User user=userRepository.getUserByUserName(userName);
		System.out.println("USER "+user);
		m.addAttribute("user",user);
	}
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal)
	
	{
		model.addAttribute("title","User Dashboard");
		
		return "normal/user_dashboard";
	}
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model)
	{
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	@PostMapping("/process-contact")
	public String processContact(
			@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, 
			Principal principal,HttpSession session)
	{
		try {
		String name=principal.getName();
		User user=this.userRepository.getUserByUserName(name);
		
		
		if(file.isEmpty())
		{
			System.out.println("File is Empty");
			contact.setImage("contact.png");
		}
		else
		{
			contact.setImage(file.getOriginalFilename());
		File saveFile=	new ClassPathResource("static/img").getFile();
		Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
		Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
		System.out.println("Image is uploaded");
		}
		contact.setUser(user);
		user.getContacts().add(contact);
		this.userRepository.save(user);
		
		System.out.println("DATA "+contact);
		System.out.println("Added to Database");
		session.setAttribute("message", new Message("Your contact is added !!Add more..","success"));
		}catch(Exception e)
		{
			System.out.println("ERROR"+e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Some went wrong !! Try again..","danger"));
		}
		return "normal/add_conatct_form";
		
	}
	@GetMapping("/show_contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m,Principal p)
	{
		m.addAttribute("title","Show User Contacts");
		String userName=p.getName();
		User user=this.userRepository.getUserByUserName(userName);
	Pageable pageable=PageRequest.of(page, 5);
	Page<Contact> contacts=	this.contactRepository.findContactsByUser(user.getId(),pageable);
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		return "normal/show_contacts";
	}
	@RequestMapping("/{cId}/contact")
	public String showContactDetails(@PathVariable("cId") Integer cId,Model model,Principal principal)
	{
		
		System.out.println("CID"+cId);
		Optional<Contact> contactOptional=this.contactRepository.findById(cId);
		Contact contact=contactOptional.get();
		
		String userName=principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		if(user.getId()==contact.getUser().getId())
		{
			model.addAttribute("contact",contact);
			model.addAttribute("title",contact.getName());
		}
		
		
		model.addAttribute("contact",contact);
		return "normal/contact_detail";
	}
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId,Model model,HttpSession session,Principal principal)
	{
		Contact contact=this.contactRepository.findById(cId).get();
		User user=this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		
		this.userRepository.save(user);
		System.out.println("DELETED");
		session.setAttribute("message", new Message("Contact deleted successfully...","success"));
		return "redirect:/user/show-contacts/0";
	}
	@PostMapping("/update-contact/{cid}")
	public String updateForm(Model m,@PathVariable("cid") Integer cid)
	{
		m.addAttribute("title","Update Contact");
		Contact contact=this.contactRepository.findById(cid).get();
		m.addAttribute("contact", contact);
			return "normal/update_form";
	}
	
	
	@RequestMapping(value="/process-update",method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model m,HttpSession session,Principal principal )
	{
		try
		{
			Contact oldContactDetails=this.contactRepository.findById(contact.getcId()).get();
			if(!file.isEmpty())
			{
				
				File deleteFile=	new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,oldContactDetails.getImage());
				file1.delete();
				
				
				
				File saveFile=	new ClassPathResource("static/img").getFile();
				Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			}
			else
			{
				contact.setImage(oldContactDetails.getImage());
			}
			User user=this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			session.setAttribute("message", new Message("Your contact is updated...","success"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("CONTACT NAME"+contact.getName());
		System.out.println("CONTACT ID"+contact.getcId());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	@GetMapping("/profile")
	public String yourProfile(Model model)
	{
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	@GetMapping("/settings")
	public String  openSetting()
	{
		return "normal/settings";
	}
	
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword ,@RequestParam("newPassword") String newPassword,Principal principal,HttpSession session)
	{
		System.out.println("OLD PASSWORD: "+oldPassword);
		System.out.println("NEW PASSWORD: "+newPassword);
		String userName=principal.getName();
		User currentUser=this.userRepository.getUserByUserName(userName);
		System.out.println(currentUser.getPassword());
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()))
		{
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your password is successfully changed...","success"));
		}
		else
		{
			session.setAttribute("message", new Message("Wrong old password !!","danger"));
			return "redirect:/user/settings";
		}
		return "redirect:/user/index";
	}
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String,Object> data,Principal principal)throws Exception
	{
		System.out.println(data);
		int amt=Integer.parseInt(data.get("amount").toString());
		var client = new RazorpayClient("your_key_id", "your_secret_id");
JSONObject ob=new JSONObject();
ob.put("amount", amt*100);
ob.put("currency", "INR");
ob.put("receipt", "txn_235425");

Order order=client.Orders.create(ob);
System.out.println(order);


MyOrder myOrder=new MyOrder();
myOrder.setAmount(order.get("amount")+"");
myOrder.setOrderId(order.get("id"));
myOrder.setPaymentId(null);
myOrder.setStatus("created");
myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
myOrder.setReceipt(order.get("receipt"));
this.myOrderRepository.save(myOrder);
//		System.out.println("Hey order function ex.");
		return order.toString();
	}
	
	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String,Object> data)
	{
		MyOrder myOrder=this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());
		this.myOrderRepository.save(myOrder);
		System.out.println(data);
		return ResponseEntity.ok(Map.of("msg","updated"));
	}
}
