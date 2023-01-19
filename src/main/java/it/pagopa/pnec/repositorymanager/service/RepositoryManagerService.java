package it.pagopa.pnec.repositorymanager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.threeten.bp.OffsetDateTime;

import it.pagopa.pnec.repositorymanager.dto.ClientConfigurationDto;
import it.pagopa.pnec.repositorymanager.dto.DigitalProgressStatusDto;
import it.pagopa.pnec.repositorymanager.dto.DigitalRequestDto;
import it.pagopa.pnec.repositorymanager.dto.DiscoveredAddressDto;
import it.pagopa.pnec.repositorymanager.dto.EventsDto;
import it.pagopa.pnec.repositorymanager.dto.GeneratedMessageDto;
import it.pagopa.pnec.repositorymanager.dto.PaperEngageRequestAttachmentsDto;
import it.pagopa.pnec.repositorymanager.dto.PaperProgressStatusDto;
import it.pagopa.pnec.repositorymanager.dto.PaperProgressStatusEventAttachmentsDto;
import it.pagopa.pnec.repositorymanager.dto.PaperRequestDto;
import it.pagopa.pnec.repositorymanager.dto.RequestDto;
import it.pagopa.pnec.repositorymanager.dto.SenderPhysicalAddressDto;
import it.pagopa.pnec.repositorymanager.model.ClientConfiguration;
import it.pagopa.pnec.repositorymanager.model.Request;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

public class RepositoryManagerService {

	public RepositoryManagerService() {

	}

	public ClientConfigurationDto insertClient(ClientConfigurationDto ccDtoI) {
		DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
		ClientConfigurationDto ccDtoO = new ClientConfigurationDto();
//		SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();
		try {
			DynamoDbTable<ClientConfiguration> clientConfigurationTable = enhancedClient.table("AnagraficaClient", TableSchema.fromBean(ClientConfiguration.class));
			
			ClientConfiguration cc = new ClientConfiguration();
			BeanUtils.copyProperties(ccDtoI, cc);

			
			if(clientConfigurationTable.getItem(cc) == null) {
				
//				spaDto.setName(cc.getSenderPhysicalAddress().getName());
//				spaDto.setAddress(cc.getSenderPhysicalAddress().getAddress());
//				spaDto.setCap(cc.getSenderPhysicalAddress().getCap());
//				spaDto.setCity(cc.getSenderPhysicalAddress().getCity());
//				spaDto.setPr(cc.getSenderPhysicalAddress().getPr());
//				
//				ccDto.setCxId(cc.getCxId());
//				ccDto.setSqsArn(cc.getSqsArn());
//				ccDto.setSqsName(cc.getSqsName());
//				ccDto.setPecReplyTo(cc.getPecReplyTo());
//				ccDto.setMailReplyTo(cc.getMailReplyTo());
//				ccDto.setSenderPhysicalAddress(spaDto);
				
				clientConfigurationTable.putItem(cc);
				System.out.println("new client data added to the table");
				BeanUtils.copyProperties(cc, ccDtoO);
				
			} else {
				System.out.println("client cannot be added to the table, id already exists");
			}
		} catch(DynamoDbException  e) {
			System.err.println(e.getMessage());
		}
		return ccDtoO;
	}

	public ClientConfigurationDto getClient(String partition_id) {
		ClientConfigurationDto ccDto = new ClientConfigurationDto();
		SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();
		
		try {
            DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();

            DynamoDbTable<ClientConfiguration> clientConfigurationTable = enhancedClient.table("AnagraficaClient", TableSchema.fromBean(ClientConfiguration.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                    		.partitionValue(partition_id)
                            .build());

            // Get items in the table and write out the ID value.
            Iterator<ClientConfiguration> result = clientConfigurationTable.query(queryConditional).items().iterator();
            ClientConfiguration cc = result.next();            
//                System.out.println("The process of the movie is "+cc.getCxId());
//                System.out.println("The target status information  is "+cc.getSqsName());
            
            spaDto.setName(cc.getSenderPhysicalAddress().getName());
			spaDto.setAddress(cc.getSenderPhysicalAddress().getAddress());
			spaDto.setCap(cc.getSenderPhysicalAddress().getCap());
			spaDto.setCity(cc.getSenderPhysicalAddress().getCity());
			spaDto.setPr(cc.getSenderPhysicalAddress().getPr());
            
            ccDto.setCxId(cc.getCxId());
            ccDto.setSqsArn(cc.getSqsArn());
            ccDto.setSqsName(cc.getSqsName());
			ccDto.setPecReplyTo(cc.getPecReplyTo());
			ccDto.setMailReplyTo(cc.getMailReplyTo());
			ccDto.setSenderPhysicalAddress(spaDto);
            
            return ccDto;

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return null;
        }
	}

	public ClientConfigurationDto updateClient(ClientConfiguration cc) {
		ClientConfigurationDto ccDto = new ClientConfigurationDto();
		SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();
		try {
			DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
			DynamoDbTable<ClientConfiguration> clientConfigurationTable = enhancedClient.table("AnagraficaClient", TableSchema.fromBean(ClientConfiguration.class));
			
			if(clientConfigurationTable.getItem(cc) != null) {
				
				spaDto.setName(cc.getSenderPhysicalAddress().getName());
				spaDto.setAddress(cc.getSenderPhysicalAddress().getAddress());
				spaDto.setCap(cc.getSenderPhysicalAddress().getCap());
				spaDto.setCity(cc.getSenderPhysicalAddress().getCity());
				spaDto.setPr(cc.getSenderPhysicalAddress().getPr());
				
				ccDto.setCxId(cc.getCxId());
				ccDto.setSqsArn(cc.getSqsArn());
				ccDto.setSqsName(cc.getSqsName());
				ccDto.setPecReplyTo(cc.getPecReplyTo());
				ccDto.setMailReplyTo(cc.getMailReplyTo());
				ccDto.setSenderPhysicalAddress(spaDto);
				
				clientConfigurationTable.putItem(cc);
				System.out.println("client data updated to the table with id ???");
			} else {
				System.out.println("id doesn't exists");
			}
		} catch(DynamoDbException  e) {
			System.err.println(e.getMessage());
		}
		return ccDto;
	}

	public ClientConfigurationDto deleteClient(String partition_id) {
		
		
		ClientConfigurationDto ccDto = new ClientConfigurationDto();
		SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();
		
		try {
            DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();

            DynamoDbTable<ClientConfiguration> clientConfigurationTable = enhancedClient.table("AnagraficaClient", TableSchema.fromBean(ClientConfiguration.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                    		.partitionValue(partition_id)
                            .build());

            // Get items in the table and write out the ID value.
            Iterator<ClientConfiguration> result = clientConfigurationTable.query(queryConditional).items().iterator();
            ClientConfiguration cc = result.next();            
//                System.out.println("The process of the movie is "+cc.getCxId());
//                System.out.println("The target status information  is "+cc.getSqsName());
            
//            BeanUtils.copyProperties(ccDtoI, cc);
            
//            spaDto.setName(cc.getSenderPhysicalAddress().getName());
//			spaDto.setAddress(cc.getSenderPhysicalAddress().getAddress());
//			spaDto.setCap(cc.getSenderPhysicalAddress().getCap());
//			spaDto.setCity(cc.getSenderPhysicalAddress().getCity());
//			spaDto.setPr(cc.getSenderPhysicalAddress().getPr());
            
            ccDto.setCxId(cc.getCxId());
            ccDto.setSqsArn(cc.getSqsArn());
            ccDto.setSqsName(cc.getSqsName());
			ccDto.setPecReplyTo(cc.getPecReplyTo());
			ccDto.setMailReplyTo(cc.getMailReplyTo());
//			ccDto.setSenderPhysicalAddress(spaDto);
            
			clientConfigurationTable.deleteItem(cc);
            
			return ccDto;

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return null;
        }
	}
	
	public RequestDto insertRequest(Request r) {
		DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
		
//		OffsetDateTime odt = OffsetDateTime.now();
		
		RequestDto rDto = new RequestDto();
			// oggetti RequestDto
			DigitalRequestDto drDto = new DigitalRequestDto();
			PaperRequestDto prDto = new PaperRequestDto();
				// oggetti PaperRequestDto
				List<PaperEngageRequestAttachmentsDto> peraDtoList = new ArrayList<PaperEngageRequestAttachmentsDto>();
				PaperEngageRequestAttachmentsDto peraDto = new PaperEngageRequestAttachmentsDto();
			List<EventsDto> eDtoList = new ArrayList<EventsDto>();
			EventsDto eDto = new EventsDto();
				// oggetti Events
				DigitalProgressStatusDto dpsDto =  new DigitalProgressStatusDto();
					// oggetti DigitalProgressStatusDto
					GeneratedMessageDto gmDto = new GeneratedMessageDto();
				PaperProgressStatusDto ppsDto = new PaperProgressStatusDto();
					// oggetti PaperProgressStatusDto
					List<PaperProgressStatusEventAttachmentsDto> ppseaDtoList = new ArrayList<PaperProgressStatusEventAttachmentsDto>();
					PaperProgressStatusEventAttachmentsDto ppseaDto = new PaperProgressStatusEventAttachmentsDto();
					DiscoveredAddressDto daDto = new DiscoveredAddressDto();
	
		try {
			DynamoDbTable<Request> requestTable = enhancedClient.table("Request", TableSchema.fromBean(Request.class));		
		
			if(requestTable.getItem(r) == null) {
				daDto.setName(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getName());
				daDto.setNameRow2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getNameRow2());
				daDto.setAddress(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getAddress());
				daDto.setAddressRow2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getName());
				daDto.setCap(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCap());
				daDto.setCity(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCity());
				daDto.setCity2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCity2());
				daDto.setPr(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getPr());
				daDto.setCountry(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCountry());
				
				ppseaDto.setId(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getId());
				ppseaDto.setDocumentType(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getDocumentType());
				ppseaDto.setUri(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getUri());
				ppseaDto.setSha256(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getSha256());
				ppseaDto.setDate(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getDate());
				
				ppseaDtoList.add(ppseaDto);
				
				ppsDto.setRegisteredLetterCode(r.getEvents().get(0).getPaperProgrStatus().getRegisteredLetterCode());
				ppsDto.setStatusCode(r.getEvents().get(0).getPaperProgrStatus().getStatusCode());
				ppsDto.setStatusDescription(r.getEvents().get(0).getPaperProgrStatus().getStatusDescription());
				ppsDto.setStatusDateTime(r.getEvents().get(0).getPaperProgrStatus().getStatusDateTime());
				ppsDto.setDeliveryFailureCause(r.getEvents().get(0).getPaperProgrStatus().getDeliveryFailureCause());
				ppsDto.setAttachments(ppseaDtoList);
				ppsDto.setDiscoveredAddress(daDto);
				ppsDto.setClientRequestTimeStamp(r.getEvents().get(0).getPaperProgrStatus().getClientRequestTimeStamp());
				
				gmDto.setSystem(r.getEvents().get(0).getDigProgrStatus().getGenMess().getSystem());
				gmDto.setId(r.getEvents().get(0).getDigProgrStatus().getGenMess().getId());
				gmDto.setLocation(r.getEvents().get(0).getDigProgrStatus().getGenMess().getLocation());
				
				dpsDto.setTimestamp(r.getEvents().get(0).getDigProgrStatus().getTimestamp());
				dpsDto.setStatus(r.getEvents().get(0).getDigProgrStatus().getStatus());
				dpsDto.setCode(r.getEvents().get(0).getDigProgrStatus().getCode());
				dpsDto.setDetails(r.getEvents().get(0).getDigProgrStatus().getDetails());
				dpsDto.setGenMess(gmDto);
				
				eDto.setDigProgrStatus(dpsDto);
				eDto.setPaperProgrStatus(ppsDto);
				
				eDtoList.add(eDto);
				
				peraDto.setUri(r.getPaperReq().getAttachments().get(0).getUri());
				peraDto.setOrder(r.getPaperReq().getAttachments().get(0).getOrder());
				peraDto.setDocumentType(r.getPaperReq().getAttachments().get(0).getDocumentType());
				peraDto.setSha256(r.getPaperReq().getAttachments().get(0).getSha256());
				
				peraDtoList.add(peraDto);
				
				Map<String, String> vas = new HashMap<String,String>();
				//TODO check
				vas.put(r.getPaperReq().getVas().toString() , r.getPaperReq().getVas().toString());
				
				prDto.setIun(r.getPaperReq().getIun());
				prDto.setRequestPaid(r.getPaperReq().getRequestPaid());
				prDto.setProductType(r.getPaperReq().getProductType());
				prDto.setAttachments(peraDtoList);
				prDto.setPrintType(r.getPaperReq().getPrintType());
				prDto.setReceiverName(r.getPaperReq().getReceiverName());
				prDto.setReceiverNameRow2(r.getPaperReq().getReceiverNameRow2());
				prDto.setReceiverAddress(r.getPaperReq().getReceiverAddress());
				prDto.setReceiverAddressRow2(r.getPaperReq().getReceiverAddressRow2());
				prDto.setReceiverCap(r.getPaperReq().getReceiverCap());
				prDto.setReceiverCity(r.getPaperReq().getReceiverCity());
				prDto.setReceiverCity2(r.getPaperReq().getReceiverCity2());
				prDto.setReceiverPr(r.getPaperReq().getReceiverPr());
				prDto.setReceiverCountry(r.getPaperReq().getReceiverCountry());
				prDto.setReceiverFiscalCode(r.getPaperReq().getReceiverFiscalCode());
				prDto.setSenderName(r.getPaperReq().getSenderName());
				prDto.setSenderAddress(r.getPaperReq().getSenderAddress());
				prDto.setSenderCity(r.getPaperReq().getSenderCity());
				prDto.setSenderPr(r.getPaperReq().getSenderPr());
				prDto.setSenderDigitalAddress(r.getPaperReq().getSenderDigitalAddress());
				prDto.setArName(r.getPaperReq().getArName());
				prDto.setArAddress(r.getPaperReq().getArAddress());
				prDto.setArCap(r.getPaperReq().getArCap());
				prDto.setArCity(r.getPaperReq().getArCity());
				prDto.setVas(vas);
				
				List<String> tagsList = new ArrayList<String>();
				String tag = r.getDigitalReq().getTags().get(0);
				tagsList.add(tag);
				
				List<String> attList = new ArrayList<String>();
				String att = r.getDigitalReq().getAttachmentsUrls().get(0);
				attList.add(att);
				
				drDto.setCorrelationId(r.getDigitalReq().getCorrelationId());
				drDto.setEventType(r.getDigitalReq().getEventType());
				drDto.setCorrelationId(r.getDigitalReq().getQos());
				drDto.setTags(tagsList);
				drDto.setClientRequestTimeStamp(r.getDigitalReq().getClientRequestTimeStamp());
				drDto.setReceiverDigitalAddress(r.getDigitalReq().getReceiverDigitalAddress());
				drDto.setMessageText(r.getDigitalReq().getMessageText());
				drDto.setSenderDigitalAddress(r.getDigitalReq().getSenderDigitalAddress());
				drDto.setChannel(r.getDigitalReq().getChannel());
				drDto.setSubjectText(r.getDigitalReq().getSubjectText());
				drDto.setMessageContentType(r.getDigitalReq().getMessageContentType());
				drDto.setAttachmentsUrls(attList);
				
				rDto.setRequestId(r.getRequestId());
				rDto.setDigitalReq(drDto);
				rDto.setPaperReq(prDto);
				rDto.setEvents(eDtoList);
			
				requestTable.putItem(r);
				System.out.println("new request data added to the table with id ???");
			} else {
				System.out.println("id already exists");
			}
		} catch(DynamoDbException  e) {
			System.err.println(e.getMessage());
		}
		
		return rDto;
	}
	
	public RequestDto getRequest (String partition_id) {
		RequestDto rDto = new RequestDto();
		// oggetti RequestDto
		DigitalRequestDto drDto = new DigitalRequestDto();
		PaperRequestDto prDto = new PaperRequestDto();
			// oggetti PaperRequestDto
			List<PaperEngageRequestAttachmentsDto> peraDtoList = new ArrayList<PaperEngageRequestAttachmentsDto>();
			PaperEngageRequestAttachmentsDto peraDto = new PaperEngageRequestAttachmentsDto();
		List<EventsDto> eDtoList = new ArrayList<EventsDto>();
		EventsDto eDto = new EventsDto();
			// oggetti Events
			DigitalProgressStatusDto dpsDto =  new DigitalProgressStatusDto();
				// oggetti DigitalProgressStatusDto
				GeneratedMessageDto gmDto = new GeneratedMessageDto();
			PaperProgressStatusDto ppsDto = new PaperProgressStatusDto();
				// oggetti PaperProgressStatusDto
				List<PaperProgressStatusEventAttachmentsDto> ppseaDtoList = new ArrayList<PaperProgressStatusEventAttachmentsDto>();
				PaperProgressStatusEventAttachmentsDto ppseaDto = new PaperProgressStatusEventAttachmentsDto();
				DiscoveredAddressDto daDto = new DiscoveredAddressDto();
		
		try {
            DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();

            DynamoDbTable<Request> requestTable = enhancedClient.table("Request", TableSchema.fromBean(Request.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                    		.partitionValue(partition_id)
                            .build());

            // Get items in the table and write out the ID value.
            Iterator<Request> result = requestTable.query(queryConditional).items().iterator();
            Request r = result.next();            

            
            daDto.setName(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getName());
			daDto.setNameRow2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getNameRow2());
			daDto.setAddress(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getAddress());
			daDto.setAddressRow2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getName());
			daDto.setCap(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCap());
			daDto.setCity(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCity());
			daDto.setCity2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCity2());
			daDto.setPr(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getPr());
			daDto.setCountry(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCountry());
			
			ppseaDto.setId(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getId());
			ppseaDto.setDocumentType(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getDocumentType());
			ppseaDto.setUri(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getUri());
			ppseaDto.setSha256(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getSha256());
			ppseaDto.setDate(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getDate());
			
			ppseaDtoList.add(ppseaDto);
			
			ppsDto.setRegisteredLetterCode(r.getEvents().get(0).getPaperProgrStatus().getRegisteredLetterCode());
			ppsDto.setStatusCode(r.getEvents().get(0).getPaperProgrStatus().getStatusCode());
			ppsDto.setStatusDescription(r.getEvents().get(0).getPaperProgrStatus().getStatusDescription());
			ppsDto.setStatusDateTime(r.getEvents().get(0).getPaperProgrStatus().getStatusDateTime());
			ppsDto.setDeliveryFailureCause(r.getEvents().get(0).getPaperProgrStatus().getDeliveryFailureCause());
			ppsDto.setAttachments(ppseaDtoList);
			ppsDto.setDiscoveredAddress(daDto);
			ppsDto.setClientRequestTimeStamp(r.getEvents().get(0).getPaperProgrStatus().getClientRequestTimeStamp());
			
			gmDto.setSystem(r.getEvents().get(0).getDigProgrStatus().getGenMess().getSystem());
			gmDto.setId(r.getEvents().get(0).getDigProgrStatus().getGenMess().getId());
			gmDto.setLocation(r.getEvents().get(0).getDigProgrStatus().getGenMess().getLocation());
			
			dpsDto.setTimestamp(r.getEvents().get(0).getDigProgrStatus().getTimestamp());
			dpsDto.setStatus(r.getEvents().get(0).getDigProgrStatus().getStatus());
			dpsDto.setCode(r.getEvents().get(0).getDigProgrStatus().getCode());
			dpsDto.setDetails(r.getEvents().get(0).getDigProgrStatus().getDetails());
			dpsDto.setGenMess(gmDto);
			
			eDto.setDigProgrStatus(dpsDto);
			eDto.setPaperProgrStatus(ppsDto);
			
			eDtoList.add(eDto);
			
			peraDto.setUri(r.getPaperReq().getAttachments().get(0).getUri());
			peraDto.setOrder(r.getPaperReq().getAttachments().get(0).getOrder());
			peraDto.setDocumentType(r.getPaperReq().getAttachments().get(0).getDocumentType());
			peraDto.setSha256(r.getPaperReq().getAttachments().get(0).getSha256());
			
			peraDtoList.add(peraDto);
			
			Map<String, String> vas = new HashMap<String,String>();
			//TODO check
			vas.put(r.getPaperReq().getVas().toString() , r.getPaperReq().getVas().toString());
			
			prDto.setIun(r.getPaperReq().getIun());
			prDto.setRequestPaid(r.getPaperReq().getRequestPaid());
			prDto.setProductType(r.getPaperReq().getProductType());
			prDto.setAttachments(peraDtoList);
			prDto.setPrintType(r.getPaperReq().getPrintType());
			prDto.setReceiverName(r.getPaperReq().getReceiverName());
			prDto.setReceiverNameRow2(r.getPaperReq().getReceiverNameRow2());
			prDto.setReceiverAddress(r.getPaperReq().getReceiverAddress());
			prDto.setReceiverAddressRow2(r.getPaperReq().getReceiverAddressRow2());
			prDto.setReceiverCap(r.getPaperReq().getReceiverCap());
			prDto.setReceiverCity(r.getPaperReq().getReceiverCity());
			prDto.setReceiverCity2(r.getPaperReq().getReceiverCity2());
			prDto.setReceiverPr(r.getPaperReq().getReceiverPr());
			prDto.setReceiverCountry(r.getPaperReq().getReceiverCountry());
			prDto.setReceiverFiscalCode(r.getPaperReq().getReceiverFiscalCode());
			prDto.setSenderName(r.getPaperReq().getSenderName());
			prDto.setSenderAddress(r.getPaperReq().getSenderAddress());
			prDto.setSenderCity(r.getPaperReq().getSenderCity());
			prDto.setSenderPr(r.getPaperReq().getSenderPr());
			prDto.setSenderDigitalAddress(r.getPaperReq().getSenderDigitalAddress());
			prDto.setArName(r.getPaperReq().getArName());
			prDto.setArAddress(r.getPaperReq().getArAddress());
			prDto.setArCap(r.getPaperReq().getArCap());
			prDto.setArCity(r.getPaperReq().getArCity());
			prDto.setVas(vas);
			
			List<String> tagsList = new ArrayList<String>();
			String tag = r.getDigitalReq().getTags().get(0);
			tagsList.add(tag);
			
			List<String> attList = new ArrayList<String>();
			String att = r.getDigitalReq().getAttachmentsUrls().get(0);
			attList.add(att);
			
			drDto.setCorrelationId(r.getDigitalReq().getCorrelationId());
			drDto.setEventType(r.getDigitalReq().getEventType());
			drDto.setCorrelationId(r.getDigitalReq().getQos());
			drDto.setTags(tagsList);
			drDto.setClientRequestTimeStamp(r.getDigitalReq().getClientRequestTimeStamp());
			drDto.setReceiverDigitalAddress(r.getDigitalReq().getReceiverDigitalAddress());
			drDto.setMessageText(r.getDigitalReq().getMessageText());
			drDto.setSenderDigitalAddress(r.getDigitalReq().getSenderDigitalAddress());
			drDto.setChannel(r.getDigitalReq().getChannel());
			drDto.setSubjectText(r.getDigitalReq().getSubjectText());
			drDto.setMessageContentType(r.getDigitalReq().getMessageContentType());
			drDto.setAttachmentsUrls(attList);
			
			rDto.setRequestId(r.getRequestId());
			rDto.setDigitalReq(drDto);
			rDto.setPaperReq(prDto);
			rDto.setEvents(eDtoList);
            
            return rDto;

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return null;
        }
	}
	
	public RequestDto updateRequest (Request r) {
DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
		
//		OffsetDateTime odt = OffsetDateTime.now();
		
		RequestDto rDto = new RequestDto();
			// oggetti RequestDto
			DigitalRequestDto drDto = new DigitalRequestDto();
			PaperRequestDto prDto = new PaperRequestDto();
				// oggetti PaperRequestDto
				List<PaperEngageRequestAttachmentsDto> peraDtoList = new ArrayList<PaperEngageRequestAttachmentsDto>();
				PaperEngageRequestAttachmentsDto peraDto = new PaperEngageRequestAttachmentsDto();
			List<EventsDto> eDtoList = new ArrayList<EventsDto>();
			EventsDto eDto = new EventsDto();
				// oggetti Events
				DigitalProgressStatusDto dpsDto =  new DigitalProgressStatusDto();
					// oggetti DigitalProgressStatusDto
					GeneratedMessageDto gmDto = new GeneratedMessageDto();
				PaperProgressStatusDto ppsDto = new PaperProgressStatusDto();
					// oggetti PaperProgressStatusDto
					List<PaperProgressStatusEventAttachmentsDto> ppseaDtoList = new ArrayList<PaperProgressStatusEventAttachmentsDto>();
					PaperProgressStatusEventAttachmentsDto ppseaDto = new PaperProgressStatusEventAttachmentsDto();
					DiscoveredAddressDto daDto = new DiscoveredAddressDto();
	
		try {
			DynamoDbTable<Request> requestTable = enhancedClient.table("Request", TableSchema.fromBean(Request.class));		
		
			if(requestTable.getItem(r) != null) {
				daDto.setName(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getName());
				daDto.setNameRow2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getNameRow2());
				daDto.setAddress(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getAddress());
				daDto.setAddressRow2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getName());
				daDto.setCap(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCap());
				daDto.setCity(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCity());
				daDto.setCity2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCity2());
				daDto.setPr(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getPr());
				daDto.setCountry(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCountry());
				
				ppseaDto.setId(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getId());
				ppseaDto.setDocumentType(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getDocumentType());
				ppseaDto.setUri(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getUri());
				ppseaDto.setSha256(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getSha256());
				ppseaDto.setDate(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getDate());
				
				ppseaDtoList.add(ppseaDto);
				
				ppsDto.setRegisteredLetterCode(r.getEvents().get(0).getPaperProgrStatus().getRegisteredLetterCode());
				ppsDto.setStatusCode(r.getEvents().get(0).getPaperProgrStatus().getStatusCode());
				ppsDto.setStatusDescription(r.getEvents().get(0).getPaperProgrStatus().getStatusDescription());
				ppsDto.setStatusDateTime(r.getEvents().get(0).getPaperProgrStatus().getStatusDateTime());
				ppsDto.setDeliveryFailureCause(r.getEvents().get(0).getPaperProgrStatus().getDeliveryFailureCause());
				ppsDto.setAttachments(ppseaDtoList);
				ppsDto.setDiscoveredAddress(daDto);
				ppsDto.setClientRequestTimeStamp(r.getEvents().get(0).getPaperProgrStatus().getClientRequestTimeStamp());
				
				gmDto.setSystem(r.getEvents().get(0).getDigProgrStatus().getGenMess().getSystem());
				gmDto.setId(r.getEvents().get(0).getDigProgrStatus().getGenMess().getId());
				gmDto.setLocation(r.getEvents().get(0).getDigProgrStatus().getGenMess().getLocation());
				
				dpsDto.setTimestamp(r.getEvents().get(0).getDigProgrStatus().getTimestamp());
				dpsDto.setStatus(r.getEvents().get(0).getDigProgrStatus().getStatus());
				dpsDto.setCode(r.getEvents().get(0).getDigProgrStatus().getCode());
				dpsDto.setDetails(r.getEvents().get(0).getDigProgrStatus().getDetails());
				dpsDto.setGenMess(gmDto);
				
				eDto.setDigProgrStatus(dpsDto);
				eDto.setPaperProgrStatus(ppsDto);
				
				eDtoList.add(eDto);
				
				peraDto.setUri(r.getPaperReq().getAttachments().get(0).getUri());
				peraDto.setOrder(r.getPaperReq().getAttachments().get(0).getOrder());
				peraDto.setDocumentType(r.getPaperReq().getAttachments().get(0).getDocumentType());
				peraDto.setSha256(r.getPaperReq().getAttachments().get(0).getSha256());
				
				peraDtoList.add(peraDto);
				
				Map<String, String> vas = new HashMap<String,String>();
				//TODO check
				vas.put(r.getPaperReq().getVas().toString() , r.getPaperReq().getVas().toString());
				
				prDto.setIun(r.getPaperReq().getIun());
				prDto.setRequestPaid(r.getPaperReq().getRequestPaid());
				prDto.setProductType(r.getPaperReq().getProductType());
				prDto.setAttachments(peraDtoList);
				prDto.setPrintType(r.getPaperReq().getPrintType());
				prDto.setReceiverName(r.getPaperReq().getReceiverName());
				prDto.setReceiverNameRow2(r.getPaperReq().getReceiverNameRow2());
				prDto.setReceiverAddress(r.getPaperReq().getReceiverAddress());
				prDto.setReceiverAddressRow2(r.getPaperReq().getReceiverAddressRow2());
				prDto.setReceiverCap(r.getPaperReq().getReceiverCap());
				prDto.setReceiverCity(r.getPaperReq().getReceiverCity());
				prDto.setReceiverCity2(r.getPaperReq().getReceiverCity2());
				prDto.setReceiverPr(r.getPaperReq().getReceiverPr());
				prDto.setReceiverCountry(r.getPaperReq().getReceiverCountry());
				prDto.setReceiverFiscalCode(r.getPaperReq().getReceiverFiscalCode());
				prDto.setSenderName(r.getPaperReq().getSenderName());
				prDto.setSenderAddress(r.getPaperReq().getSenderAddress());
				prDto.setSenderCity(r.getPaperReq().getSenderCity());
				prDto.setSenderPr(r.getPaperReq().getSenderPr());
				prDto.setSenderDigitalAddress(r.getPaperReq().getSenderDigitalAddress());
				prDto.setArName(r.getPaperReq().getArName());
				prDto.setArAddress(r.getPaperReq().getArAddress());
				prDto.setArCap(r.getPaperReq().getArCap());
				prDto.setArCity(r.getPaperReq().getArCity());
				prDto.setVas(vas);
				
				List<String> tagsList = new ArrayList<String>();
				String tag = r.getDigitalReq().getTags().get(0);
				tagsList.add(tag);
				
				List<String> attList = new ArrayList<String>();
				String att = r.getDigitalReq().getAttachmentsUrls().get(0);
				attList.add(att);
				
				drDto.setCorrelationId(r.getDigitalReq().getCorrelationId());
				drDto.setEventType(r.getDigitalReq().getEventType());
				drDto.setCorrelationId(r.getDigitalReq().getQos());
				drDto.setTags(tagsList);
				drDto.setClientRequestTimeStamp(r.getDigitalReq().getClientRequestTimeStamp());
				drDto.setReceiverDigitalAddress(r.getDigitalReq().getReceiverDigitalAddress());
				drDto.setMessageText(r.getDigitalReq().getMessageText());
				drDto.setSenderDigitalAddress(r.getDigitalReq().getSenderDigitalAddress());
				drDto.setChannel(r.getDigitalReq().getChannel());
				drDto.setSubjectText(r.getDigitalReq().getSubjectText());
				drDto.setMessageContentType(r.getDigitalReq().getMessageContentType());
				drDto.setAttachmentsUrls(attList);
				
				rDto.setRequestId(r.getRequestId());
				rDto.setDigitalReq(drDto);
				rDto.setPaperReq(prDto);
				rDto.setEvents(eDtoList);
			
				requestTable.putItem(r);
				System.out.println("new request data added to the table with id ???");
			} else {
				System.out.println("id already exists");
			}
		} catch(DynamoDbException  e) {
			System.err.println(e.getMessage());
		}
		
		return rDto;
	}
	
	public RequestDto deleteRequest (String partition_id) {
		RequestDto rDto = new RequestDto();
		// oggetti RequestDto
		DigitalRequestDto drDto = new DigitalRequestDto();
		PaperRequestDto prDto = new PaperRequestDto();
			// oggetti PaperRequestDto
			List<PaperEngageRequestAttachmentsDto> peraDtoList = new ArrayList<PaperEngageRequestAttachmentsDto>();
			PaperEngageRequestAttachmentsDto peraDto = new PaperEngageRequestAttachmentsDto();
		List<EventsDto> eDtoList = new ArrayList<EventsDto>();
		EventsDto eDto = new EventsDto();
			// oggetti Events
			DigitalProgressStatusDto dpsDto =  new DigitalProgressStatusDto();
				// oggetti DigitalProgressStatusDto
				GeneratedMessageDto gmDto = new GeneratedMessageDto();
			PaperProgressStatusDto ppsDto = new PaperProgressStatusDto();
				// oggetti PaperProgressStatusDto
				List<PaperProgressStatusEventAttachmentsDto> ppseaDtoList = new ArrayList<PaperProgressStatusEventAttachmentsDto>();
				PaperProgressStatusEventAttachmentsDto ppseaDto = new PaperProgressStatusEventAttachmentsDto();
				DiscoveredAddressDto daDto = new DiscoveredAddressDto();
		
		try {
            DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();

            DynamoDbTable<Request> requestTable = enhancedClient.table("Request", TableSchema.fromBean(Request.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                    		.partitionValue(partition_id)
                            .build());

            // Get items in the table and write out the ID value.
            Iterator<Request> result = requestTable.query(queryConditional).items().iterator();
            Request r = result.next();            

            
            daDto.setName(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getName());
			daDto.setNameRow2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getNameRow2());
			daDto.setAddress(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getAddress());
			daDto.setAddressRow2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getName());
			daDto.setCap(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCap());
			daDto.setCity(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCity());
			daDto.setCity2(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCity2());
			daDto.setPr(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getPr());
			daDto.setCountry(r.getEvents().get(0).getPaperProgrStatus().getDiscoveredAddress().getCountry());
			
			ppseaDto.setId(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getId());
			ppseaDto.setDocumentType(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getDocumentType());
			ppseaDto.setUri(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getUri());
			ppseaDto.setSha256(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getSha256());
			ppseaDto.setDate(r.getEvents().get(0).getPaperProgrStatus().getAttachments().get(0).getDate());
			
			ppseaDtoList.add(ppseaDto);
			
			ppsDto.setRegisteredLetterCode(r.getEvents().get(0).getPaperProgrStatus().getRegisteredLetterCode());
			ppsDto.setStatusCode(r.getEvents().get(0).getPaperProgrStatus().getStatusCode());
			ppsDto.setStatusDescription(r.getEvents().get(0).getPaperProgrStatus().getStatusDescription());
			ppsDto.setStatusDateTime(r.getEvents().get(0).getPaperProgrStatus().getStatusDateTime());
			ppsDto.setDeliveryFailureCause(r.getEvents().get(0).getPaperProgrStatus().getDeliveryFailureCause());
			ppsDto.setAttachments(ppseaDtoList);
			ppsDto.setDiscoveredAddress(daDto);
			ppsDto.setClientRequestTimeStamp(r.getEvents().get(0).getPaperProgrStatus().getClientRequestTimeStamp());
			
			gmDto.setSystem(r.getEvents().get(0).getDigProgrStatus().getGenMess().getSystem());
			gmDto.setId(r.getEvents().get(0).getDigProgrStatus().getGenMess().getId());
			gmDto.setLocation(r.getEvents().get(0).getDigProgrStatus().getGenMess().getLocation());
			
			dpsDto.setTimestamp(r.getEvents().get(0).getDigProgrStatus().getTimestamp());
			dpsDto.setStatus(r.getEvents().get(0).getDigProgrStatus().getStatus());
			dpsDto.setCode(r.getEvents().get(0).getDigProgrStatus().getCode());
			dpsDto.setDetails(r.getEvents().get(0).getDigProgrStatus().getDetails());
			dpsDto.setGenMess(gmDto);
			
			eDto.setDigProgrStatus(dpsDto);
			eDto.setPaperProgrStatus(ppsDto);
			
			eDtoList.add(eDto);
			
			peraDto.setUri(r.getPaperReq().getAttachments().get(0).getUri());
			peraDto.setOrder(r.getPaperReq().getAttachments().get(0).getOrder());
			peraDto.setDocumentType(r.getPaperReq().getAttachments().get(0).getDocumentType());
			peraDto.setSha256(r.getPaperReq().getAttachments().get(0).getSha256());
			
			peraDtoList.add(peraDto);
			
			Map<String, String> vas = new HashMap<String,String>();
			//TODO check
			vas.put(r.getPaperReq().getVas().toString() , r.getPaperReq().getVas().toString());
			
			prDto.setIun(r.getPaperReq().getIun());
			prDto.setRequestPaid(r.getPaperReq().getRequestPaid());
			prDto.setProductType(r.getPaperReq().getProductType());
			prDto.setAttachments(peraDtoList);
			prDto.setPrintType(r.getPaperReq().getPrintType());
			prDto.setReceiverName(r.getPaperReq().getReceiverName());
			prDto.setReceiverNameRow2(r.getPaperReq().getReceiverNameRow2());
			prDto.setReceiverAddress(r.getPaperReq().getReceiverAddress());
			prDto.setReceiverAddressRow2(r.getPaperReq().getReceiverAddressRow2());
			prDto.setReceiverCap(r.getPaperReq().getReceiverCap());
			prDto.setReceiverCity(r.getPaperReq().getReceiverCity());
			prDto.setReceiverCity2(r.getPaperReq().getReceiverCity2());
			prDto.setReceiverPr(r.getPaperReq().getReceiverPr());
			prDto.setReceiverCountry(r.getPaperReq().getReceiverCountry());
			prDto.setReceiverFiscalCode(r.getPaperReq().getReceiverFiscalCode());
			prDto.setSenderName(r.getPaperReq().getSenderName());
			prDto.setSenderAddress(r.getPaperReq().getSenderAddress());
			prDto.setSenderCity(r.getPaperReq().getSenderCity());
			prDto.setSenderPr(r.getPaperReq().getSenderPr());
			prDto.setSenderDigitalAddress(r.getPaperReq().getSenderDigitalAddress());
			prDto.setArName(r.getPaperReq().getArName());
			prDto.setArAddress(r.getPaperReq().getArAddress());
			prDto.setArCap(r.getPaperReq().getArCap());
			prDto.setArCity(r.getPaperReq().getArCity());
			prDto.setVas(vas);
			
			List<String> tagsList = new ArrayList<String>();
			String tag = r.getDigitalReq().getTags().get(0);
			tagsList.add(tag);
			
			List<String> attList = new ArrayList<String>();
			String att = r.getDigitalReq().getAttachmentsUrls().get(0);
			attList.add(att);
			
			drDto.setCorrelationId(r.getDigitalReq().getCorrelationId());
			drDto.setEventType(r.getDigitalReq().getEventType());
			drDto.setCorrelationId(r.getDigitalReq().getQos());
			drDto.setTags(tagsList);
			drDto.setClientRequestTimeStamp(r.getDigitalReq().getClientRequestTimeStamp());
			drDto.setReceiverDigitalAddress(r.getDigitalReq().getReceiverDigitalAddress());
			drDto.setMessageText(r.getDigitalReq().getMessageText());
			drDto.setSenderDigitalAddress(r.getDigitalReq().getSenderDigitalAddress());
			drDto.setChannel(r.getDigitalReq().getChannel());
			drDto.setSubjectText(r.getDigitalReq().getSubjectText());
			drDto.setMessageContentType(r.getDigitalReq().getMessageContentType());
			drDto.setAttachmentsUrls(attList);
			
			rDto.setRequestId(r.getRequestId());
			rDto.setDigitalReq(drDto);
			rDto.setPaperReq(prDto);
			rDto.setEvents(eDtoList);
            
			requestTable.deleteItem(r);
			
            return rDto;

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return null;
        }
	}
	
}
